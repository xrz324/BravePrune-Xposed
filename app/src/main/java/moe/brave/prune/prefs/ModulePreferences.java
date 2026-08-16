package moe.brave.prune.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class ModulePreferences {
    public static final String FILE_NAME = "brave_prune";
    public static final String KEY_LANGUAGE_FINGERPRINTING = "language_fingerprinting";
    public static final String KEY_FINGERPRINTING_DEFAULT_APPLIED =
            "default_fingerprinting_v1";
    public static final String KEY_HTTPS_DEFAULT_APPLIED = "default_https_v1";
    public static final String KEY_SHIELDS_DEFAULT_APPLIED = "default_shields_v1";
    public static final String KEY_WEBRTC_DEFAULT_APPLIED = "default_webrtc_v1";

    private ModulePreferences() {}

    public static SharedPreferences from(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
