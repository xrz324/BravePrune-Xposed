package moe.brave.prune.prefs;

import moe.brave.prune.compat.BraveRuntime;
import moe.brave.prune.compat.Reflect;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ProductPreferenceAdapter {
    private static final Map<ClassLoader, EnforcementState> STATES = new WeakHashMap<>();

    private final ClassLoader loader;
    private final EnforcementState state;

    public ProductPreferenceAdapter(ClassLoader loader) {
        this.loader = loader;
        synchronized (STATES) {
            state = STATES.computeIfAbsent(loader, ignored -> new EnforcementState());
        }
    }

    public void enforce() {
        enforceLocalState();
        try {
            Object profile = BraveRuntime.lastUsedRegularProfile(loader);
            enforceProfile(profile);
        } catch (Throwable ignored) {
            // Profile startup can race the first callback; profile hooks and resume retry it.
        }
        enforceAndroidPreferences();
    }

    public void enforceLocalState() {
        if (state.isLocalStateApplied()) {
            return;
        }
        try {
            Object localState = localState();
            setIntegersBestEffort(
                    localState, ProductPreferenceOverrides.localStateIntegers());
            commitLocalState();
            state.markLocalStateApplied();
        } catch (Throwable ignored) {
            // Retry after native browser initialization.
        }
    }

    public void enforceProfile(Object profile) {
        if (profile == null) {
            return;
        }
        if (state.isProfileApplied(profile)) {
            return;
        }
        try {
            Object profilePrefs = BraveRuntime.profilePrefs(loader, profile);
            setBooleansBestEffort(
                    profilePrefs, ProductPreferenceOverrides.profileBooleans());
            setIntegersBestEffort(
                    profilePrefs, ProductPreferenceOverrides.profileIntegers());
            state.markProfileApplied(profile);
        } catch (Throwable ignored) {
            // A later profile callback or activity resume retries the preference set.
        }
    }

    public void enforceAndroidPreferences() {
        if (state.isAndroidPreferencesApplied()) {
            return;
        }
        try {
            Object preferences = Reflect.callStatic(
                    loader,
                    "org.chromium.chrome.browser.preferences.ChromeSharedPreferences",
                    "getInstance");
            Reflect.call(
                    preferences,
                    "writeBoolean",
                    "pref_brave_wallet_web3_notifications",
                    false);
            state.markAndroidPreferencesApplied();
        } catch (Throwable ignored) {
            // Retry after ChromeSharedPreferences becomes visible.
        }
    }

    private Object localState() throws Exception {
        return BraveRuntime.localState(loader);
    }

    private static void setInteger(Object prefService, String key, int value)
            throws Exception {
        BraveRuntime.setIntegerPreference(prefService, key, value);
    }

    private static void setBooleansBestEffort(
            Object prefService, Map<String, Boolean> values) {
        values.forEach((key, value) -> {
            try {
                BraveRuntime.setBooleanPreference(prefService, key, value);
            } catch (Throwable ignored) {
                // Product pref sets vary across Brave channels and releases.
            }
        });
    }

    private static void setIntegersBestEffort(
            Object prefService, Map<String, Integer> values) {
        values.forEach((key, value) -> {
            try {
                setInteger(prefService, key, value);
            } catch (Throwable ignored) {
                // Product pref sets vary across Brave channels and releases.
            }
        });
    }

    private void commitLocalState() throws Exception {
        BraveRuntime.commitLocalState(loader);
    }

    private static final class EnforcementState {
        private final Set<Object> appliedProfiles = Collections.newSetFromMap(
                new WeakHashMap<>());
        private boolean localStateApplied;
        private boolean androidPreferencesApplied;

        synchronized boolean isLocalStateApplied() {
            return localStateApplied;
        }

        synchronized void markLocalStateApplied() {
            localStateApplied = true;
        }

        synchronized boolean isAndroidPreferencesApplied() {
            return androidPreferencesApplied;
        }

        synchronized void markAndroidPreferencesApplied() {
            androidPreferencesApplied = true;
        }

        synchronized boolean isProfileApplied(Object profile) {
            return appliedProfiles.contains(profile);
        }

        synchronized void markProfileApplied(Object profile) {
            appliedProfiles.add(profile);
        }
    }
}
