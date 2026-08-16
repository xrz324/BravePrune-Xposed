package moe.brave.prune.hook;

import moe.brave.prune.compat.BraveSymbols;
import moe.brave.prune.feature.FeatureOverrides;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class FeatureListHooks {
    private FeatureListHooks() {}

    public static int install(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        int installed = 0;
        for (String className : BraveSymbols.FEATURE_LIST_CLASSES) {
            try {
                Class<?> featureList = Class.forName(className, false, loader);
                for (Method method : featureList.getDeclaredMethods()) {
                    if (!Modifier.isStatic(method.getModifiers())
                            || method.getReturnType() != boolean.class
                            || method.getParameterCount() != 1
                            || method.getParameterTypes()[0] != String.class) {
                        continue;
                    }
                    method.setAccessible(true);
                    handles.add(module.hook(method).intercept(chain -> {
                        String feature = (String) chain.getArg(0);
                        if (FeatureOverrides.isForcedEnabled(feature)) return true;
                        if (FeatureOverrides.isForcedDisabled(feature)) return false;
                        return chain.proceed();
                    }));
                    installed++;
                }
            } catch (ClassNotFoundException ignored) {
                // Each Brave/Chromium generation may expose only one of these wrappers.
            }
        }
        return installed;
    }
}
