package moe.brave.prune.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.Test;

public final class BraveRuntimeTest {
    @Test
    public void preferenceSettersAreResolvedAfterR8RenameAndReorder() throws Exception {
        FakePrefService preferences = new FakePrefService();

        BraveRuntime.setBooleanPreference(preferences, "boolean.pref", true);
        BraveRuntime.setIntegerPreference(preferences, "integer.pref", 7);
        BraveRuntime.setStringPreference(preferences, "string.pref", "value");

        assertEquals("boolean.pref", preferences.booleanKey);
        assertTrue(preferences.booleanValue);
        assertEquals("integer.pref", preferences.integerKey);
        assertEquals(7, preferences.integerValue);
        assertEquals("string.pref", preferences.stringKey);
        assertEquals("value", preferences.stringValue);
    }

    @Test
    public void capturedProfilePreferencesExcludeLocalState() {
        Object localState = new Object();
        Object profilePreferences = new Object();

        assertEquals(profilePreferences, BraveRuntime.selectProfilePrefService(
                List.of(localState, profilePreferences), localState));
        assertNull(BraveRuntime.selectProfilePrefService(List.of(localState), localState));
    }

    @Test
    public void browserInitializationFailuresAreRetryable() {
        Throwable initializationRace = new InvocationTargetException(
                new IllegalStateException("Browser hasn't finished initialization yet!"));
        Throwable missingNativeLibrary = new InvocationTargetException(
                new UnsatisfiedLinkError("is the library loaded, e.g. System.loadLibrary?"));

        assertTrue(BraveRuntime.isTransientNotReady(initializationRace));
        assertTrue(BraveRuntime.isTransientNotReady(missingNativeLibrary));
        assertFalse(BraveRuntime.isTransientNotReady(
                new NoSuchMethodException("upstream changed")));
    }

    private static final class FakePrefService {
        String booleanKey;
        boolean booleanValue;
        String integerKey;
        int integerValue;
        String stringKey;
        String stringValue;

        void a(boolean value, String key) {
            booleanKey = key;
            booleanValue = value;
        }

        void b(String key, int value) {
            integerKey = key;
            integerValue = value;
        }

        void c(String key, String value) {
            stringKey = key;
            stringValue = value;
        }
    }
}
