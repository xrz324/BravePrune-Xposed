package moe.brave.prune.feature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class FeatureOverridesTest {
    @Test
    public void mergeEnabledPreservesExistingFeaturesAndRemovesDisabledOnes() {
        String merged = FeatureOverrides.mergeEnabled(
                "ExistingFeature,AIChat<Study,BraveRoundTimeStamps:OldTrial");

        assertTrue(merged.contains("ExistingFeature"));
        assertTrue(merged.contains("BraveRoundTimeStamps"));
        assertFalse(merged.contains("AIChat<Study"));
        assertFalse(merged.contains("BraveRoundTimeStamps:OldTrial"));
    }

    @Test
    public void mergeDisabledRemovesEnabledConflictsAndAddsProductFeatures() {
        String merged = FeatureOverrides.mergeDisabled(
                "ExistingFeature,BraveAdblockDefault1pBlocking<Study");

        assertTrue(merged.contains("ExistingFeature"));
        assertTrue(merged.contains("AIChat"));
        assertTrue(merged.contains("BraveRewards"));
        assertFalse(merged.contains("BraveAdblockDefault1pBlocking<Study"));
    }

    @Test
    public void adsStartupDelayFeatureIsNotTreatedAsAnAdsKillSwitch() {
        assertFalse(FeatureOverrides.isForcedDisabled("InProcessBraveAdsService"));
        assertFalse(containsFeature(
                FeatureOverrides.mergeDisabled(""), "InProcessBraveAdsService"));
    }

    @Test
    public void aiFeatureFamiliesAreForcedOffAcrossRenames() {
        assertTrue(FeatureOverrides.isForcedDisabled("AIChatFirst"));
        assertTrue(FeatureOverrides.isForcedDisabled("BraveAIChatFutureFeature"));
        assertTrue(FeatureOverrides.isForcedDisabled("LocalAIExperimental"));
        assertTrue(FeatureOverrides.isForcedDisabled("HistoryEmbeddings"));
        assertTrue(FeatureOverrides.isForcedDisabled("HistoryEmbeddingsAnswers"));
        assertFalse(FeatureOverrides.isForcedDisabled("UnrelatedFeature"));
    }

    @Test
    public void walletFeatureFamiliesAreForcedOffAcrossRenames() {
        assertTrue(FeatureOverrides.isForcedDisabled("BraveWalletBitcoin"));
        assertTrue(FeatureOverrides.isForcedDisabled("BraveWalletZCashFeature"));
        assertTrue(FeatureOverrides.isForcedDisabled("BraveWalletFutureChain"));
        assertFalse(FeatureOverrides.isForcedDisabled("BraveShields"));
    }

    @Test
    public void nativeWalletFeaturesAreAddedToDisabledCommandLine() {
        String merged = FeatureOverrides.mergeDisabled("");

        assertTrue(merged.contains("BraveWalletBitcoin"));
        assertTrue(merged.contains("BraveWalletZCash"));
        assertTrue(merged.contains("BraveWalletCardano"));
        assertTrue(merged.contains("BraveWalletMojoForHardwareWalletFeature"));
        assertFalse(containsFeature(merged, "BraveWalletBitcoinFeature"));
    }

    @Test
    public void vpnFeatureFamiliesAreAlwaysForcedOff() {
        assertTrue(FeatureOverrides.isForcedDisabled("BraveVPN"));
        assertTrue(FeatureOverrides.isForcedDisabled("BraveVPNFutureAndroidUI"));
        assertTrue(containsFeature(FeatureOverrides.mergeDisabled(""), "BraveVPN"));
    }

    @Test
    public void syncFeatureIsLeftUnchanged() {
        assertFalse(FeatureOverrides.isForcedDisabled("BraveSync"));
        assertFalse(containsFeature(FeatureOverrides.mergeDisabled(""), "BraveSync"));
        assertTrue(containsFeature(
                FeatureOverrides.mergeDisabled("BraveSync"), "BraveSync"));
    }

    private static boolean containsFeature(String features, String expected) {
        return Arrays.asList(features.split(",")).contains(expected);
    }
}
