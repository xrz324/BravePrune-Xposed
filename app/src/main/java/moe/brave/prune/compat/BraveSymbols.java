package moe.brave.prune.compat;

import java.util.List;
import java.util.Set;

public final class BraveSymbols {
    public static final Set<String> PACKAGES = Set.of(
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.brave.browser_dev",
            "com.brave.browser_nightly",
            "com.brave.browser_default");

    public static final List<String> FEATURE_LIST_CLASSES = List.of(
            "org.chromium.chrome.browser.flags.ChromeFeatureList",
            "org.chromium.base.FeatureList");

    public static final List<String> PRIVACY_SETTINGS_CLASSES = List.of(
            "org.chromium.chrome.browser.privacy.settings.BravePrivacySettings",
            "org.chromium.chrome.browser.settings.BravePrivacySettings");

    public static final List<String> BROWSER_ACTIVITY_CLASSES = List.of(
            "org.chromium.chrome.browser.app.BraveActivity",
            "org.chromium.chrome.browser.ChromeTabbedActivity");

    public static final List<String> WALLET_PROVIDER_DELEGATE_CLASSES = List.of(
            "org.chromium.chrome.browser.crypto_wallet.BraveWalletProviderDelegateImplHelper");

    public static final String COMMAND_LINE = "org.chromium.base.CommandLine";
    public static final String BRAVE_COMMAND_LINE_INIT =
            "org.chromium.base.BraveCommandLineInitUtil";
    public static final String SPLIT_CHROME_APPLICATION =
            "org.chromium.chrome.browser.base.SplitChromeApplication";
    public static final String BRAVE_LOCAL_STATE =
            "org.chromium.chrome.browser.BraveLocalState";
    public static final String LOCAL_STATE_PREFS =
            "org.chromium.chrome.browser.prefs.LocalStatePrefs";
    public static final String PROFILE_MANAGER =
            "org.chromium.chrome.browser.profiles.ProfileManager";
    public static final String USER_PREFS = "org.chromium.components.user_prefs.UserPrefs";
    public static final String SHIELDS_CONTENT_SETTINGS =
            "org.chromium.chrome.browser.preferences.website.BraveShieldsContentSettings";
    public static final String BRAVE_VPN_SERVICE =
            "org.chromium.chrome.browser.vpn.wireguard.WireguardService";

    private BraveSymbols() {}
}
