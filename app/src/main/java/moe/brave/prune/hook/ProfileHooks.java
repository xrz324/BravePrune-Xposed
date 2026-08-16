package moe.brave.prune.hook;

import android.content.Context;

import moe.brave.prune.compat.BraveRuntime;
import moe.brave.prune.compat.BraveSymbols;
import moe.brave.prune.prefs.BravePreferenceAdapter;
import moe.brave.prune.prefs.ProductPreferenceAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class ProfileHooks {
    private ProfileHooks() {}

    public static int install(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        try {
            Class<?> profileManager = Class.forName(
                    BraveSymbols.PROFILE_MANAGER, false, loader);
            Class<?> profileType = Class.forName(
                    "org.chromium.chrome.browser.profiles.Profile", false, loader);
            ProductPreferenceAdapter adapter = new ProductPreferenceAdapter(loader);
            BravePreferenceAdapter defaults = new BravePreferenceAdapter(module, loader);
            adapter.enforceLocalState();
            adapter.enforceAndroidPreferences();

            int installed = 0;
            for (Method method : profileManager.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() == 0
                        && profileType.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    handles.add(module.hook(method).intercept(chain -> {
                        Object profile = chain.proceed();
                        BraveRuntime.recordProfile(loader, profile);
                        adapter.enforceProfile(profile);
                        return profile;
                    }));
                    installed++;
                } else if (method.getReturnType() == void.class
                        && method.getParameterCount() == 1
                        && profileType.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    handles.add(module.hook(method).intercept(chain -> {
                        Object profile = chain.getArg(0);
                        Object result = chain.proceed();
                        BraveRuntime.recordProfile(loader, profile);
                        Context context = ManagedPolicyHooks.applicationContext();
                        if (context != null) {
                            defaults.apply(context, profile);
                        } else {
                            adapter.enforceProfile(profile);
                        }
                        return result;
                    }));
                    installed++;
                }
            }
            return installed;
        } catch (ClassNotFoundException ignored) {
            return 0;
        }
    }

}
