package moe.brave.prune.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import moe.brave.prune.compat.BraveRuntime;
import moe.brave.prune.hook.ManagedPolicyHooks;

import io.github.libxposed.api.XposedModule;

public final class BravePreferenceAdapter {
    private static final String TAG = "BravePrune";

    private final XposedModule module;
    private final ClassLoader loader;
    private final ProductPreferenceAdapter productPreferences;

    public BravePreferenceAdapter(XposedModule module, ClassLoader loader) {
        this.module = module;
        this.loader = loader;
        productPreferences = new ProductPreferenceAdapter(loader);
    }

    public void apply(Context context) {
        SharedPreferences modulePreferences = ModulePreferences.from(context);
        productPreferences.enforce();
        applyLanguageProtection(modulePreferences.getBoolean(
                ModulePreferences.KEY_LANGUAGE_FINGERPRINTING, false));
        try {
            applyInitialDefaults(
                    modulePreferences, BraveRuntime.lastUsedRegularProfile(loader));
        } catch (Throwable error) {
            if (!BraveRuntime.isTransientNotReady(error)) {
                module.log(Log.ERROR, TAG, "Regular profile was unavailable", error);
            }
        }
    }

    public void apply(Context context, Object profile) {
        SharedPreferences modulePreferences = ModulePreferences.from(context);
        productPreferences.enforceLocalState();
        productPreferences.enforceProfile(profile);
        productPreferences.enforceAndroidPreferences();
        applyLanguageProtection(modulePreferences.getBoolean(
                ModulePreferences.KEY_LANGUAGE_FINGERPRINTING, false));
        applyInitialDefaults(modulePreferences, profile);
    }

    public void applyLanguageProtection(boolean enabled) {
        ManagedPolicyHooks.setLanguageProtectionEnabled(enabled);
    }

    private void applyInitialDefaults(SharedPreferences preferences, Object profile) {
        applyOnce(preferences, ModulePreferences.KEY_FINGERPRINTING_DEFAULT_APPLIED,
                "fingerprinting=block", () ->
            setShieldValue(profile, "fingerprinting", "block"));
        applyOnce(preferences, ModulePreferences.KEY_HTTPS_DEFAULT_APPLIED,
                "httpsUpgrade=block", () ->
            setShieldValue(profile, "httpsUpgrade", "block"));
        applyOnce(preferences, ModulePreferences.KEY_SHIELDS_DEFAULT_APPLIED,
                "trackers=block", () ->
            setShieldValue(profile, "trackers", "block"));
        applyOnce(preferences, ModulePreferences.KEY_WEBRTC_DEFAULT_APPLIED,
                "webrtc=disable_non_proxied_udp", () -> setWebrtcDefault(profile));
    }

    private void setShieldValue(Object profile, String resource, String value) throws Exception {
        BraveRuntime.setGlobalShieldValue(loader, profile, resource, value);
    }

    private void setWebrtcDefault(Object profile) throws Exception {
        Object preferences = BraveRuntime.profilePrefs(loader, profile);
        BraveRuntime.setStringPreference(
                preferences, "webrtc.ip_handling_policy", "disable_non_proxied_udp");
    }

    private void applyOnce(
            SharedPreferences preferences,
            String key,
            String description,
            PreferenceAction action) {
        if (preferences.getBoolean(key, false)) {
            return;
        }
        try {
            action.run();
            preferences.edit().putBoolean(key, true).apply();
        } catch (Throwable error) {
            if (!BraveRuntime.isTransientNotReady(error)) {
                module.log(Log.ERROR, TAG, "Default unavailable: " + description, error);
            }
        }
    }

    @FunctionalInterface
    private interface PreferenceAction {
        void run() throws Exception;
    }
}
