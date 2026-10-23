package moe.brave.prune.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LanguagePreferenceOverridesTest {
    @Test
    public void disablingProtectionAlsoExemptsStrictModeNavigatorLanguages() throws Exception {
        FakePrefService prefs = new FakePrefService();

        LanguagePreferenceOverrides.apply(prefs, false);

        assertEquals("brave.reduce_language", prefs.booleanKey);
        assertFalse(prefs.reduced);
        assertEquals("profile.default_content_setting_values.brave_webcompat_language",
                prefs.integerKey);
        assertEquals(1, prefs.languageException);
    }

    @Test
    public void enablingProtectionRestoresLanguageFarbling() throws Exception {
        FakePrefService prefs = new FakePrefService();
        LanguagePreferenceOverrides.apply(prefs, false);

        LanguagePreferenceOverrides.apply(prefs, true);

        assertTrue(prefs.reduced);
        assertEquals(2, prefs.languageException);
    }

    private static final class FakePrefService {
        String booleanKey;
        boolean reduced;
        String integerKey;
        int languageException;

        @SuppressWarnings("unused")
        private void f(String key, boolean value) {
            booleanKey = key;
            reduced = value;
        }

        @SuppressWarnings("unused")
        private void g(int value, String key) {
            integerKey = key;
            languageException = value;
        }
    }
}
