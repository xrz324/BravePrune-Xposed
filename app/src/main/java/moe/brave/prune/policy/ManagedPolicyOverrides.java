package moe.brave.prune.policy;

import java.util.Map;

public final class ManagedPolicyOverrides {
    private static final Map<String, Boolean> VALUES = Map.ofEntries(
            Map.entry("BraveWalletDisabled", true),
            Map.entry("BraveRewardsDisabled", true),
            Map.entry("BraveNewsDisabled", true),
            Map.entry("BraveVPNDisabled", true),
            Map.entry("BraveAIChatEnabled", false),
            Map.entry("BraveLocalAIEnabled", false),
            Map.entry("BraveP3AEnabled", false),
            Map.entry("BraveStatsPingEnabled", false),
            Map.entry("BraveWebDiscoveryEnabled", false),
            Map.entry("MetricsReportingEnabled", false));

    private ManagedPolicyOverrides() {}

    public static Map<String, Boolean> values() {
        return VALUES;
    }
}
