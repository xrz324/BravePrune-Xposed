package moe.brave.prune.feature;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class FeatureOverrides {
    private static final List<String> ENABLED = List.of(
            "BraveAdblockShowHiddenComponents",
            "BraveShowStrictFingerprintingMode",
            "BraveAdblockDefault1pBlocking",
            "BraveRoundTimeStamps",
            "HttpsByDefault");

    private static final List<String> DISABLED = List.of(
            "AIChat",
            "AIChatHistory",
            "AIChatAgentProfile",
            "AIChatWebUIEnabled",
            "ShowAIChatInputOnNewTabPage",
            "HistoryEmbeddings",
            "HistoryEmbeddingsAnswers",
            "LaunchedHistoryEmbeddings",
            "BraveNewsCardPeek",
            "BraveNewsSidebar",
            "BraveNewsFeedUpdate",
            "BraveRewards",
            "BraveVPN",
            "BraveVPNExperimentalV2",
            "BraveVPNLinkSubscriptionAndroidUI",
            "BraveWallet",
            "BraveWalletBitcoin",
            "BraveWalletBitcoinImport",
            "BraveWalletBitcoinLedger",
            "BraveWalletZCash",
            "BraveWalletPolkadot",
            "BraveWalletDebug",
            "BraveWalletCardano",
            "BraveWalletAnkrBalances",
            "BraveWalletTransactionSimulations",
            "BraveWalletMojoForHardwareWalletFeature",
            "BraveWalletAccountHiding",
            "BraveWalletSidePanel",
            "BraveWalletWebUIFeature");

    private FeatureOverrides() {}

    public static boolean isForcedEnabled(String feature) {
        return ENABLED.contains(baseName(feature));
    }

    public static boolean isForcedDisabled(String feature) {
        String name = baseName(feature);
        if (DISABLED.contains(name)) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.startsWith("bravewallet")
                || lower.startsWith("aichat")
                || lower.startsWith("braveaichat")
                || lower.startsWith("bravelocalai")
                || lower.startsWith("localai")
                || lower.startsWith("bravevpn");
    }

    public static String mergeEnabled(String current) {
        LinkedHashSet<String> values = parse(current);
        values.removeIf(feature -> isForcedEnabled(feature) || isForcedDisabled(feature));
        values.addAll(ENABLED);
        return String.join(",", values);
    }

    public static String mergeDisabled(String current) {
        LinkedHashSet<String> values = parse(current);
        values.removeIf(feature -> isForcedEnabled(feature) || isForcedDisabled(feature));
        values.addAll(DISABLED);
        return String.join(",", values);
    }

    private static LinkedHashSet<String> parse(String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String entry : value.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String baseName(String feature) {
        if (feature == null) {
            return "";
        }
        int parameter = feature.indexOf('<');
        int trial = feature.indexOf(':');
        int end = feature.length();
        if (parameter >= 0) end = Math.min(end, parameter);
        if (trial >= 0) end = Math.min(end, trial);
        return feature.substring(0, end);
    }
}
