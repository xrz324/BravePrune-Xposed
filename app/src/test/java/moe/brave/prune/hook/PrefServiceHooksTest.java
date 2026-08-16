package moe.brave.prune.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.junit.Test;

public final class PrefServiceHooksTest {
    @Test
    public void walletProvidersAreForcedToNone() {
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.wallet.default_wallet2"));
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.wallet.default_solana_wallet"));
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.wallet.default_cardano_wallet"));
    }

    @Test
    public void decentralizedResolversAreForcedOff() {
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.unstoppable_domains.resolve_method"));
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.ens.resolve_method"));
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.ens.offchain_resolve_method"));
        assertEquals(Integer.valueOf(1),
                PrefServiceHooks.forcedInteger("brave.sns.resolve_method"));
        assertNull(PrefServiceHooks.forcedInteger("unrelated.pref"));
    }

    @Test
    public void onlyNonPolicyProductWritesAreCoerced() {
        assertEquals(Boolean.FALSE,
                PrefServiceHooks.forcedBoolean("brave.rewards.enabled"));
        assertEquals(Boolean.FALSE,
                PrefServiceHooks.forcedBoolean(
                        "brave.new_tab_page.sponsored_images.survey_panelist"));
        assertNull(PrefServiceHooks.forcedBoolean(
                "brave.ad_block.checked_all_default_regions"));
        assertNull(PrefServiceHooks.forcedBoolean("brave.rewards.disabled_by_policy"));
        assertNull(PrefServiceHooks.forcedBoolean("brave.p3a.enabled"));
    }

    @Test
    public void r8ReorderedSettersAreMatchedByStructure() throws Exception {
        Method booleanSetter = FakePrefService.class.getDeclaredMethod(
                "booleanSetter", boolean.class, String.class);
        Method integerSetter = FakePrefService.class.getDeclaredMethod(
                "integerSetter", int.class, String.class);
        Method stringSetter = FakePrefService.class.getDeclaredMethod(
                "stringSetter", String.class, String.class);

        assertTrue(PrefServiceHooks.isBooleanSetter(booleanSetter));
        assertTrue(PrefServiceHooks.isIntegerSetter(integerSetter));
        assertTrue(PrefServiceHooks.isStringSetter(stringSetter));
        assertFalse(PrefServiceHooks.isIntegerGetter(integerSetter));
    }

    @Test
    public void nativeFactoryIsMatchedByStructure() throws Exception {
        Method factory = FakePrefService.class.getDeclaredMethod("create", long.class);

        assertTrue(PrefServiceHooks.isFactory(factory, FakePrefService.class));
        assertFalse(PrefServiceHooks.isFactory(
                FakePrefService.class.getDeclaredMethod("b", String.class),
                FakePrefService.class));
    }

    @Test
    public void onlyUnambiguousIntegerQueriesAreMatchedByStructure() throws Exception {
        Method booleanQuery = FakePrefService.class.getDeclaredMethod(
                "a", String.class);
        Method integerQuery = FakePrefService.class.getDeclaredMethod(
                "b", String.class);

        assertFalse(PrefServiceHooks.isBooleanSetter(booleanQuery));
        assertTrue(PrefServiceHooks.isIntegerGetter(integerQuery));
    }

    private static final class FakePrefService {
        static FakePrefService create(long nativePointer) {
            return new FakePrefService();
        }

        boolean a(String key) {
            return false;
        }

        int b(String key) {
            return 0;
        }

        void booleanSetter(boolean value, String key) {}

        void integerSetter(int value, String key) {}

        void stringSetter(String key, String value) {}
    }
}
