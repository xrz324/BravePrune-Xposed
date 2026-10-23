package moe.brave.prune.prefs;

import moe.brave.prune.compat.BraveRuntime;

public final class LanguagePreferenceOverrides {
    private static final String REDUCE_LANGUAGE = "brave.reduce_language";
    private static final String LANGUAGE_WEBCOMPAT_DEFAULT =
            "profile.default_content_setting_values.brave_webcompat_language";
    private static final int CONTENT_SETTING_ALLOW = 1;
    private static final int CONTENT_SETTING_BLOCK = 2;

    private LanguagePreferenceOverrides() {}

    public static void apply(Object profilePrefs, boolean enabled) throws Exception {
        BraveRuntime.setBooleanPreference(profilePrefs, REDUCE_LANGUAGE, enabled);
        // Strict-mode navigator.languages uses the language WebCompat rule independently
        // of reduce_language. Allowing this exception disables only language farbling.
        BraveRuntime.setIntegerPreference(profilePrefs, LANGUAGE_WEBCOMPAT_DEFAULT,
                enabled ? CONTENT_SETTING_BLOCK : CONTENT_SETTING_ALLOW);
    }
}
