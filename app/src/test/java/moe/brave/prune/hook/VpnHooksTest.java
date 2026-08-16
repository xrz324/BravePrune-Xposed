package moe.brave.prune.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VpnHooksTest {
    @Test
    public void blocksOnlyTheBraveWireguardService() {
        assertTrue(VpnHooks.isBlockedService(
                "org.chromium.chrome.browser.vpn.wireguard.WireguardService"));
        assertFalse(VpnHooks.isBlockedService(
                "com.wireguard.android.backend.GoBackend$VpnService"));
        assertFalse(VpnHooks.isBlockedService(null));
    }
}
