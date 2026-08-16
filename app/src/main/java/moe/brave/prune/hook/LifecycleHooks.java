package moe.brave.prune.hook;

import android.content.Context;
import android.util.Log;

import moe.brave.prune.compat.BraveSymbols;
import moe.brave.prune.compat.BraveRuntime;
import moe.brave.prune.compat.Reflect;
import moe.brave.prune.prefs.BravePreferenceAdapter;
import moe.brave.prune.settings.SettingsInjector;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class LifecycleHooks {
    private static final String TAG = "BravePrune";

    private LifecycleHooks() {}

    public static int installActivity(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        BravePreferenceAdapter adapter = new BravePreferenceAdapter(module, loader);
        return installActivityHook(module, loader, handles, adapter);
    }

    public static int installPrivacySettings(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        BravePreferenceAdapter adapter = new BravePreferenceAdapter(module, loader);
        SettingsInjector injector = new SettingsInjector(loader, adapter);
        return installPrivacySettingsHook(module, loader, handles, adapter, injector);
    }

    private static int installActivityHook(
            XposedModule module,
            ClassLoader loader,
            List<HookHandle> handles,
            BravePreferenceAdapter adapter) {
        for (String className : BraveSymbols.BROWSER_ACTIVITY_CLASSES) {
            try {
                Class<?> activity = Class.forName(className, false, loader);
                List<Method> candidates = Reflect.methods(activity, "onResume", 0);
                if (candidates.isEmpty()) {
                    continue;
                }
                handles.add(module.hook(candidates.get(0)).intercept(chain -> {
                    Object result = chain.proceed();
                    if (chain.getThisObject() instanceof Context context) {
                        try {
                            adapter.apply(context);
                        } catch (Throwable error) {
                            module.log(Log.ERROR, TAG, "Preference defaults were skipped", error);
                        }
                    }
                    return result;
                }));
                return 1;
            } catch (ClassNotFoundException ignored) {
                // Continue through compatibility aliases.
            }
        }
        return 0;
    }

    private static int installPrivacySettingsHook(
            XposedModule module,
            ClassLoader loader,
            List<HookHandle> handles,
            BravePreferenceAdapter adapter,
            SettingsInjector injector) {
        for (String className : BraveSymbols.PRIVACY_SETTINGS_CLASSES) {
            try {
                Class<?> fragment = Class.forName(className, false, loader);
                Method candidate = settingsCreationMethod(fragment);
                if (candidate == null) {
                    continue;
                }
                handles.add(module.hook(candidate).intercept(chain -> {
                    Object result = chain.proceed();
                    Context context;
                    try {
                        context = BraveRuntime.context(chain.getThisObject());
                    } catch (Throwable error) {
                        module.log(Log.ERROR, TAG, "Settings context was unavailable", error);
                        return result;
                    }
                    try {
                        adapter.apply(context);
                    } catch (Throwable error) {
                        module.log(Log.ERROR, TAG, "Preference defaults were skipped", error);
                    }
                    try {
                        injector.inject(chain.getThisObject(), context);
                    } catch (Throwable error) {
                        module.log(Log.ERROR, TAG, "Settings injection was skipped", error);
                    }
                    return result;
                }));
                return 1;
            } catch (ClassNotFoundException ignored) {
                // Continue through compatibility aliases.
            }
        }
        return 0;
    }

    private static Method settingsCreationMethod(Class<?> fragment) {
        for (Method method : fragment.getDeclaredMethods()) {
            if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
                continue;
            }
            boolean bundle = false;
            boolean string = false;
            for (Class<?> parameter : method.getParameterTypes()) {
                bundle |= parameter == android.os.Bundle.class;
                string |= parameter == String.class;
            }
            if (bundle && string) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
