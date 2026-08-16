package moe.brave.prune;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import moe.brave.prune.compat.BraveSymbols;
import moe.brave.prune.hook.CommandLineHooks;
import moe.brave.prune.hook.FeatureListHooks;
import moe.brave.prune.hook.LifecycleHooks;
import moe.brave.prune.hook.ManagedPolicyHooks;
import moe.brave.prune.hook.PrefServiceHooks;
import moe.brave.prune.hook.ProfileHooks;
import moe.brave.prune.hook.VpnHooks;
import moe.brave.prune.hook.WalletHooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    private static final String TAG = "BravePrune";
    private final List<HookHandle> handles = new ArrayList<>();
    private final Map<String, Integer> installedAdapters = new HashMap<>();
    private String processName = "";
    private boolean bootstrapInstalled;
    private boolean splitSupplierInstalled;
    private boolean installed;
    private int commandLineHookCount;
    private String lastCompatibilityFailure = "";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        boolean isMainProcess = param.getPackageName().equals(processName);
        if (bootstrapInstalled
                || !param.isFirstPackage()
                || !BraveSymbols.PACKAGES.contains(param.getPackageName())
                || !isMainProcess) {
            return;
        }

        int managedPolicies = install("managed-policies", () ->
                ManagedPolicyHooks.install(this, param.getClassLoader(), handles));
        ensureCommandLineHooks(param.getClassLoader());
        int splitContext = attempt(() ->
                installSplitContextBootstrap(param.getClassLoader()));
        int application = attempt(this::installApplicationBootstrap);
        bootstrapInstalled = managedPolicies > 0 && (splitContext > 0 || application > 0);
        if (splitContext == 0 && application == 0) {
            log(Log.ERROR, TAG, "No browser bootstrap adapter is available");
        }

        installBraveHooks(param.getClassLoader(), null, "package-ready");
    }

    private int installSplitContextBootstrap(ClassLoader loader) throws Exception {
        Class<?> application = Class.forName(
                BraveSymbols.SPLIT_CHROME_APPLICATION, false, loader);
        Method method = application.getDeclaredMethod(
                "createContextForSplit", String.class);
        method.setAccessible(true);
        handles.add(hook(method).intercept(chain -> {
            Object result = chain.proceed();
            if ("chrome".equals(chain.getArg(0)) && result instanceof Context context) {
                installBraveHooks(
                        context.getClassLoader(), context, "chrome-split-context");
            }
            return result;
        }));
        return 1;
    }

    private int installApplicationBootstrap() throws Exception {
        Method method = Instrumentation.class.getDeclaredMethod(
                "callApplicationOnCreate", Application.class);
        method.setAccessible(true);
        handles.add(hook(method).intercept(chain -> {
            Application application = (Application) chain.getArg(0);
            ManagedPolicyHooks.initialize(application);
            ensureCommandLineHooks(application.getClassLoader());
            installSplitSupplierBootstrap(application);
            installBraveHooks(
                    application.getClassLoader(), application, "application-onCreate");
            return chain.proceed();
        }));
        return 1;
    }

    private synchronized void installSplitSupplierBootstrap(Application application) {
        if (splitSupplierInstalled) {
            return;
        }
        try {
            Supplier<?> supplier = findSplitSupplier(application);
            Method get = supplier.getClass().getDeclaredMethod("get");
            get.setAccessible(true);
            handles.add(hook(get).intercept(chain -> {
                Object delegate = chain.proceed();
                if (delegate != null) {
                    installBraveHooks(
                            delegate.getClass().getClassLoader(),
                            application,
                            "chrome-split-delegate");
                }
                return delegate;
            }));
            splitSupplierInstalled = true;
        } catch (Throwable ignored) {
            // The split-context and Application hooks remain available as bootstraps.
        }
    }

    private static Supplier<?> findSplitSupplier(Application application) throws Exception {
        for (Class<?> current = application.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || !Supplier.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(application);
                if (value instanceof Supplier<?> supplier) {
                    return supplier;
                }
            }
        }
        throw new NoSuchFieldException(application.getClass().getName() + " split supplier");
    }

    private synchronized void installBraveHooks(
            ClassLoader loader, Context context, String phase) {
        if (installed) {
            return;
        }
        if (!isBraveCodeVisible(loader)) {
            return;
        }

        if (context != null) {
            ManagedPolicyHooks.initialize(context);
        }
        int commandLine = ensureCommandLineHooks(loader);
        installAdapter("feature-list", () ->
                FeatureListHooks.install(this, loader, handles));
        int preferences = installAdapter("preferences", () ->
                PrefServiceHooks.install(this, loader, handles));
        installAdapter("profiles", () ->
                ProfileHooks.install(this, loader, handles));
        installAdapter("vpn", () ->
                VpnHooks.install(this, loader, handles));
        installAdapter("wallet", () ->
                WalletHooks.install(this, loader, handles));
        int activity = installAdapter("activity-lifecycle", () ->
                LifecycleHooks.installActivity(this, loader, handles));
        int settings = installAdapter("settings-lifecycle", () ->
                LifecycleHooks.installPrivacySettings(this, loader, handles));

        installed = commandLine > 0 && preferences > 0 && activity > 0 && settings > 0;
        if (!installed) {
            String failure = "commandLine=" + commandLine
                    + ", preferences=" + preferences
                    + ", activity=" + activity
                    + ", settings=" + settings;
            if (!failure.equals(lastCompatibilityFailure)) {
                lastCompatibilityFailure = failure;
                log(Log.ERROR, TAG, "Critical compatibility adapter missing during " + phase
                    + ": commandLine=" + commandLine
                    + ", preferences=" + preferences
                    + ", activity=" + activity
                    + ", settings=" + settings);
            }
        }
    }

    private synchronized int ensureCommandLineHooks(ClassLoader loader) {
        if (commandLineHookCount > 0) {
            return commandLineHookCount;
        }
        commandLineHookCount = installAdapter("command-line", () ->
                CommandLineHooks.install(this, loader, handles));
        return commandLineHookCount;
    }

    private synchronized int installAdapter(String name, Installer installer) {
        Integer installedCount = installedAdapters.get(name);
        if (installedCount != null) {
            return installedCount;
        }
        int count = install(name, installer);
        if (count > 0) {
            installedAdapters.put(name, count);
        }
        return count;
    }

    private static boolean isBraveCodeVisible(ClassLoader loader) {
        try {
            Class.forName("org.chromium.components.prefs.PrefService", false, loader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private int install(String name, Installer installer) {
        try {
            return installer.install();
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Adapter unavailable: " + name, error);
            return 0;
        }
    }

    private static int attempt(Installer installer) {
        try {
            return installer.install();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface Installer {
        int install() throws Exception;
    }
}
