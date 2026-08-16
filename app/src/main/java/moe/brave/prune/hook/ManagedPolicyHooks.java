package moe.brave.prune.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import moe.brave.prune.compat.BraveSymbols;
import moe.brave.prune.policy.ManagedPolicyOverrides;
import moe.brave.prune.prefs.ModulePreferences;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class ManagedPolicyHooks {
    private static volatile Context applicationContext;
    private static volatile boolean languageProtectionEnabled;

    private ManagedPolicyHooks() {}

    public static int install(
            XposedModule module, ClassLoader ignored, List<HookHandle> handles) throws Exception {
        Method method = UserManager.class.getDeclaredMethod(
                "getApplicationRestrictions", String.class);
        method.setAccessible(true);
        handles.add(module.hook(method).intercept(chain -> {
            Object result = chain.proceed();
            String packageName = (String) chain.getArg(0);
            if (!BraveSymbols.PACKAGES.contains(packageName)) {
                return result;
            }
            return enforce(result instanceof Bundle bundle ? bundle : null);
        }));
        return 1;
    }

    public static void initialize(Context context) {
        Context resolvedContext = context.getApplicationContext();
        applicationContext = resolvedContext != null ? resolvedContext : context;
        var preferences = ModulePreferences.from(applicationContext);
        setLanguageProtectionEnabled(preferences.getBoolean(
                ModulePreferences.KEY_LANGUAGE_FINGERPRINTING, false));
    }

    public static void setLanguageProtectionEnabled(boolean enabled) {
        languageProtectionEnabled = enabled;
    }

    public static Context applicationContext() {
        return applicationContext;
    }

    static Bundle enforce(Bundle source) {
        Bundle result = source == null ? new Bundle() : new Bundle(source);
        ManagedPolicyOverrides.values().forEach(result::putBoolean);
        result.putBoolean("BraveReduceLanguageEnabled", languageProtectionEnabled);
        return result;
    }
}
