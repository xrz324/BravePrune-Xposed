package moe.brave.prune.hook;

import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;

import moe.brave.prune.compat.BraveSymbols;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class VpnHooks {
    private VpnHooks() {}

    public static int install(
            XposedModule module, ClassLoader ignored, List<HookHandle> handles) throws Exception {
        int installed = 0;
        for (String name : List.of("startForegroundService", "startService")) {
            Method method = ContextWrapper.class.getDeclaredMethod(name, Intent.class);
            method.setAccessible(true);
            handles.add(module.hook(method).intercept(chain -> {
                Intent intent = (Intent) chain.getArg(0);
                ComponentName component = intent == null ? null : intent.getComponent();
                if (component != null && isBlockedService(component.getClassName())) {
                    return component;
                }
                return chain.proceed();
            }));
            installed++;
        }
        return installed;
    }

    static boolean isBlockedService(String className) {
        return BraveSymbols.BRAVE_VPN_SERVICE.equals(className);
    }
}
