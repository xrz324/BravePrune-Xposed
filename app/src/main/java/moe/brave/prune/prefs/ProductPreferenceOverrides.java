package moe.brave.prune.prefs;

import java.util.Map;

public final class ProductPreferenceOverrides {
    private static final Map<String, Boolean> PROFILE_BOOLEANS = Map.ofEntries(
            Map.entry("brave.rewards.enabled", false),
            Map.entry("brave.rewards.ac.enabled", false),
            Map.entry("brave.brave_ads.notifications.enabled", false),
            Map.entry("brave.brave_ads.opted_in_to_search_result_ads", false),
            Map.entry("brave.brave_ads.enabled_last_profile", false),
            Map.entry("brave.brave_ads.ever_enabled_any_profile", false),
            Map.entry("brave.new_tab_page.show_brave_news", false),
            Map.entry("brave.today.opted_in", false),
            Map.entry("brave.today.should_show_toolbar_button", false),
            Map.entry("brave.new_tab_page.show_branded_background_image", false),
            Map.entry("brave.new_tab_page.sponsored_images.survey_panelist", false),
            Map.entry("brave.ai_chat.storage_enabled", false),
            Map.entry("brave.ai_chat.autocomplete_provider_enabled", false),
            Map.entry("brave.ai_chat.context_menu_enabled", false),
            Map.entry("brave.ai_chat.show_toolbar_button", false),
            Map.entry("brave.ai_chat.tab_organization_enabled", false),
            Map.entry("brave.ai_chat.ntp_input_day_zero_enabled", false),
            Map.entry("brave.history_embeddings_enabled", false),
            Map.entry("brave.brave_vpn.show_button", false),
            Map.entry("brave.wallet.should_show_wallet_suggestion_badge", false),
            Map.entry("brave.wallet.show_wallet_icon_on_toolbar", false),
            Map.entry("brave.wallet.nft_discovery_enabled", false),
            Map.entry("brave.wallet.private_windows_enabled", false));

    private static final Map<String, Integer> PROFILE_INTEGERS = Map.of(
            "brave.wallet.default_wallet2", 1,
            "brave.wallet.default_solana_wallet", 1,
            "brave.wallet.default_cardano_wallet", 1);

    private static final Map<String, Integer> LOCAL_STATE_INTEGERS = Map.of(
            "brave.unstoppable_domains.resolve_method", 1,
            "brave.ens.resolve_method", 1,
            "brave.ens.offchain_resolve_method", 1,
            "brave.sns.resolve_method", 1);

    private ProductPreferenceOverrides() {}

    public static Map<String, Boolean> profileBooleans() {
        return PROFILE_BOOLEANS;
    }

    public static Map<String, Integer> profileIntegers() {
        return PROFILE_INTEGERS;
    }

    public static Map<String, Integer> localStateIntegers() {
        return LOCAL_STATE_INTEGERS;
    }

    public static Boolean booleanValue(String key) {
        return PROFILE_BOOLEANS.get(key);
    }

    public static Integer integerValue(String key) {
        Integer profileValue = PROFILE_INTEGERS.get(key);
        return profileValue != null ? profileValue : LOCAL_STATE_INTEGERS.get(key);
    }
}
