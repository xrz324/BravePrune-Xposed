package moe.brave.prune.hook;

import android.util.Log;

import moe.brave.prune.compat.BraveRuntime;
import moe.brave.prune.compat.BraveSymbols;
import moe.brave.prune.feature.FeatureOverrides;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class CommandLineHooks {
    private static final String ENABLE_FEATURES = "enable-features";
    private static final String DISABLE_FEATURES = "disable-features";
    private static boolean applyFailureLogged;

    private CommandLineHooks() {}

    public static int install(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) throws Exception {
        int installed = installBraveInitializer(module, loader, handles);
        installed += installChromiumFallback(module, loader, handles);
        return installed;
    }

    private static int installBraveInitializer(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        try {
            Class<?> initializer = Class.forName(
                    BraveSymbols.BRAVE_COMMAND_LINE_INIT, false, loader);
            int installed = 0;
            for (Method method : initializer.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())
                        || method.getReturnType() != void.class
                        || (method.getParameterCount() != 0
                                && !containsParameter(method, String.class))) {
                    continue;
                }
                method.setAccessible(true);
                handles.add(module.hook(method).intercept(chain -> {
                    Object result = chain.proceed();
                    applyOrLog(module, loader);
                    return result;
                }));
                installed++;
            }
            return installed;
        } catch (ClassNotFoundException ignored) {
            return 0;
        }
    }

    private static int installChromiumFallback(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) throws Exception {
        Class<?> commandLine = Class.forName(BraveSymbols.COMMAND_LINE, false, loader);
        int installed = 0;
        for (Method method : commandLine.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != void.class
                    || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != String[].class) {
                continue;
            }
            method.setAccessible(true);
            handles.add(module.hook(method).intercept(chain -> {
                Object result = chain.proceed();
                applyOrLog(module, loader);
                return result;
            }));
            installed++;
        }
        return installed;
    }

    private static void applyOrLog(XposedModule module, ClassLoader loader) {
        try {
            apply(loader);
        } catch (Throwable error) {
            if (BraveRuntime.isTransientNotReady(error)) {
                return;
            }
            synchronized (CommandLineHooks.class) {
                if (applyFailureLogged) {
                    return;
                }
                applyFailureLogged = true;
            }
            module.log(Log.ERROR, "BravePrune", "Command-line overrides were skipped", error);
        }
    }

    private static void apply(ClassLoader loader) throws Exception {
        Class<?> type = Class.forName(BraveSymbols.COMMAND_LINE, false, loader);
        Object commandLine = instance(type);
        if (commandLine == null) {
            throw new IllegalStateException("CommandLine is not initialized");
        }
        Method getSwitchValue = findMethod(type, String.class, String.class);
        Method appendSwitchWithValue = findMethod(
                type, void.class, String.class, String.class);
        String enabled = (String) getSwitchValue.invoke(commandLine, ENABLE_FEATURES);
        String disabled = (String) getSwitchValue.invoke(commandLine, DISABLE_FEATURES);
        appendIfChanged(
                commandLine,
                getSwitchValue,
                appendSwitchWithValue,
                ENABLE_FEATURES,
                FeatureOverrides.mergeEnabled(enabled));
        appendIfChanged(
                commandLine,
                getSwitchValue,
                appendSwitchWithValue,
                DISABLE_FEATURES,
                FeatureOverrides.mergeDisabled(disabled));
    }

    private static void appendIfChanged(
            Object commandLine,
            Method getSwitchValue,
            Method appendSwitchWithValue,
            String name,
            String value) throws Exception {
        if (!Objects.equals(getSwitchValue.invoke(commandLine, name), value)) {
            appendSwitchWithValue.invoke(commandLine, name, value);
        }
    }

    private static Object instance(Class<?> type) throws IllegalAccessException {
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == type) {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Method findMethod(
            Class<?> type, Class<?> returnType, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == returnType
                    && java.util.Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName());
    }

    private static boolean containsParameter(Method method, Class<?> type) {
        for (Class<?> parameter : method.getParameterTypes()) {
            if (parameter == type) {
                return true;
            }
        }
        return false;
    }
}
