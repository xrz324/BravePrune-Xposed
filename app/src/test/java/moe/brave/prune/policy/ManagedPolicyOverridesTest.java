package moe.brave.prune.policy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ManagedPolicyOverridesTest {
    @Test
    public void nativeProductPoliciesUseRestrictiveValues() {
        assertEquals(Boolean.TRUE,
                ManagedPolicyOverrides.values().get("BraveWalletDisabled"));
        assertEquals(Boolean.TRUE,
                ManagedPolicyOverrides.values().get("BraveRewardsDisabled"));
        assertEquals(Boolean.TRUE,
                ManagedPolicyOverrides.values().get("BraveNewsDisabled"));
        assertEquals(Boolean.TRUE,
                ManagedPolicyOverrides.values().get("BraveVPNDisabled"));
        assertEquals(Boolean.FALSE,
                ManagedPolicyOverrides.values().get("BraveAIChatEnabled"));
        assertEquals(Boolean.FALSE,
                ManagedPolicyOverrides.values().get("BraveLocalAIEnabled"));
        assertEquals(Boolean.FALSE,
                ManagedPolicyOverrides.values().get("BraveP3AEnabled"));
        assertEquals(Boolean.FALSE,
                ManagedPolicyOverrides.values().get("BraveStatsPingEnabled"));
        assertEquals(Boolean.FALSE,
                ManagedPolicyOverrides.values().get("BraveWebDiscoveryEnabled"));
        assertEquals(Boolean.FALSE,
                ManagedPolicyOverrides.values().get("MetricsReportingEnabled"));
    }
}
