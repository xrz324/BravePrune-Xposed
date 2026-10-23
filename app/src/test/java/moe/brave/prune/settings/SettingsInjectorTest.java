package moe.brave.prune.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SettingsInjectorTest {
    @Test
    public void bindsSourceListenerSetterWhenItIsRetained() throws Exception {
        SourcePreference preference = new SourcePreference();
        SettingsInjector.ListenerBinding binding =
                SettingsInjector.listenerBinding(preference, FakePreference.class);
        ChangeListener listener = (changedPreference, value) -> true;

        binding.bind(preference, listener);

        assertEquals(ChangeListener.class, binding.listenerType());
        assertSame(listener, preference.listener);
        assertTrue(preference.setterCalled);
    }

    @Test
    public void bindsListenerFieldWhenR8RemovesSetter() throws Exception {
        R8Preference preference = new R8Preference();
        SettingsInjector.ListenerBinding binding =
                SettingsInjector.listenerBinding(preference, FakePreference.class);
        MergedChangeListener listener = (changedPreference, value) -> true;

        binding.bind(preference, listener);

        assertEquals(MergedChangeListener.class, binding.listenerType());
        assertSame(listener, preference.y);
    }

    @Test
    public void ignoresOneArgumentClickListenerField() throws Exception {
        R8Preference preference = new R8Preference();
        SettingsInjector.ListenerBinding binding =
                SettingsInjector.listenerBinding(preference, FakePreference.class);

        assertEquals(MergedChangeListener.class, binding.listenerType());
    }

    @Test
    public void acceptsR8OptimizedVoidPreferenceAddMethod() throws Exception {
        FakeScreen screen = new FakeScreen();
        FakePreference preference = new FakePreference();
        SettingsInjector.setKey(preference, FakePreference.class,
                "brave_prune_language_fingerprinting");

        SettingsInjector.addPreference(screen, preference, FakePreference.class);

        assertSame(preference, screen.item);
        assertEquals("brave_prune_language_fingerprinting", screen.lastLookupKey);
    }

    @Test
    public void keySetterIgnoresSubclassStringSetter() throws Exception {
        ShadowedKeyPreference preference = new ShadowedKeyPreference();

        SettingsInjector.setKey(preference, FakePreference.class, "module_key");

        assertEquals("module_key", ((FakePreference) preference).key);
        assertNull(preference.summary);
    }

    @Test
    public void titleSetterIgnoresSubclassSummarySetter() throws Exception {
        ShadowedKeyPreference preference = new ShadowedKeyPreference();

        SettingsInjector.setTitle(preference, FakePreference.class, "Language protection");

        assertEquals("Language protection", ((FakePreference) preference).title);
        assertNull(preference.summary);
    }

    private interface ChangeListener {
        boolean onPreferenceChange(FakePreference preference, Object newValue);
    }

    private interface ParentChangeListener {
        boolean F1(FakePreference preference, Object newValue);
    }

    private interface MergedChangeListener extends ParentChangeListener {}

    private interface ClickListener {
        boolean d2(FakePreference preference);
    }

    private static class FakePreference {
        private String key;
        private CharSequence title;

        @SuppressWarnings("unused")
        private void x0(String key) {
            this.key = key;
        }

        @SuppressWarnings("unused")
        private void L0(CharSequence title) {
            this.title = title;
        }
    }

    private static final class SourcePreference extends FakePreference {
        private ChangeListener listener;
        private boolean setterCalled;

        @SuppressWarnings("unused")
        private void renamedSetter(ChangeListener listener) {
            this.listener = listener;
            setterCalled = true;
        }
    }

    private static final class R8Preference extends FakePreference {
        private ClickListener z;
        private MergedChangeListener y;
    }

    private static final class ShadowedKeyPreference extends FakePreference {
        private CharSequence summary;

        @SuppressWarnings("unused")
        private void a1(String value) {
            summary = value;
        }

        @SuppressWarnings("unused")
        private void Z0(CharSequence value) {
            summary = value;
        }
    }

    private static final class FakeScreen {
        private FakePreference item;
        private CharSequence lastLookupKey;

        @SuppressWarnings("unused")
        private FakePreference findPreference(CharSequence key) {
            lastLookupKey = key;
            return item != null && key.equals(item.key) ? item : null;
        }

        @SuppressWarnings("unused")
        private void a1(FakePreference preference) {
            item = preference;
        }
    }
}
