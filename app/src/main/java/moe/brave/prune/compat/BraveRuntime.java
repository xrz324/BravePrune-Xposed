package moe.brave.prune.compat;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class BraveRuntime {
    private static final List<String> LOCAL_STATE_CLASSES = List.of(
            BraveSymbols.LOCAL_STATE_PREFS,
            BraveSymbols.BRAVE_LOCAL_STATE);
    private static final Map<ClassLoader, List<WeakReference<Object>>> PREF_SERVICES =
            new WeakHashMap<>();
    private static final Map<ClassLoader, WeakReference<Object>> REGULAR_PROFILES =
            new WeakHashMap<>();

    private BraveRuntime() {}

    public static Object lastUsedRegularProfile(ClassLoader loader) throws Exception {
        Object recorded = recordedProfile(loader);
        if (recorded != null) {
            return recorded;
        }
        Class<?> manager = Class.forName(BraveSymbols.PROFILE_MANAGER, false, loader);
        Class<?> profile = Class.forName(
                "org.chromium.chrome.browser.profiles.Profile", false, loader);
        for (Method method : manager.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 0
                    && profile.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                Object profileValue = method.invoke(null);
                recordProfile(loader, profileValue);
                return profileValue;
            }
        }
        throw new NoSuchMethodException(manager.getName() + " profile getter");
    }

    public static Object localState(ClassLoader loader) throws Exception {
        Class<?> prefService = Class.forName(
                "org.chromium.components.prefs.PrefService", false, loader);
        for (String className : LOCAL_STATE_CLASSES) {
            try {
                Class<?> type = Class.forName(className, false, loader);
                for (Method method : type.getDeclaredMethods()) {
                    if (!Modifier.isStatic(method.getModifiers())
                            || method.getParameterCount() != 0
                            || !prefService.isAssignableFrom(method.getReturnType())) {
                        continue;
                    }
                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // Continue through the Chromium and legacy Brave aliases.
            }
        }
        throw new IllegalStateException("Native local-state PrefService is not ready");
    }

    public static Object profilePrefs(ClassLoader loader, Object profile) throws Exception {
        Class<?> prefService = Class.forName(
                "org.chromium.components.prefs.PrefService", false, loader);
        try {
            Class<?> userPrefs = Class.forName(BraveSymbols.USER_PREFS, false, loader);
            for (Method method : userPrefs.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 1
                        || !prefService.isAssignableFrom(method.getReturnType())
                        || !method.getParameterTypes()[0].isInstance(profile)) {
                    continue;
                }
                method.setAccessible(true);
                Object result = method.invoke(null, profile);
                if (result != null) {
                    recordPrefService(loader, result);
                    return result;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Release builds may merge and rename UserPrefs. Use captured PrefService wrappers.
        }

        Object localState = localState(loader);
        Object captured = capturedProfilePrefs(loader, localState);
        if (captured != null) {
            return captured;
        }
        throw new BrowserNotReadyException("Profile PrefService has not been created yet");
    }

    public static void recordPrefService(ClassLoader loader, Object prefService) {
        if (loader == null || prefService == null) {
            return;
        }
        synchronized (PREF_SERVICES) {
            List<WeakReference<Object>> services = PREF_SERVICES.computeIfAbsent(
                    loader, ignored -> new ArrayList<>());
            for (int i = services.size() - 1; i >= 0; i--) {
                Object existing = services.get(i).get();
                if (existing == null) {
                    services.remove(i);
                } else if (existing == prefService) {
                    return;
                }
            }
            services.add(new WeakReference<>(prefService));
        }
    }

    public static void recordProfile(ClassLoader loader, Object profile) {
        if (loader == null || profile == null) {
            return;
        }
        synchronized (REGULAR_PROFILES) {
            REGULAR_PROFILES.put(loader, new WeakReference<>(profile));
        }
    }

    public static boolean isTransientNotReady(Throwable error) {
        for (Throwable current = error; current != null && current != current.getCause();
                current = current.getCause()) {
            if (current instanceof BrowserNotReadyException) {
                return true;
            }
            String message = current.getMessage();
            if (current instanceof IllegalStateException
                    && message != null
                    && (message.contains("hasn't finished initialization")
                            || message.contains("not initialized")
                            || message.contains("is not ready"))) {
                return true;
            }
            if (current instanceof UnsatisfiedLinkError
                    && message != null
                    && message.contains("is the library loaded")) {
                return true;
            }
        }
        return false;
    }

    public static void setBooleanPreference(Object prefService, String key, boolean value)
            throws Exception {
        invokePreferenceSetter(prefService, key, value, boolean.class);
    }

    public static void setIntegerPreference(Object prefService, String key, int value)
            throws Exception {
        invokePreferenceSetter(prefService, key, value, int.class);
    }

    public static void setStringPreference(Object prefService, String key, String value)
            throws Exception {
        invokePreferenceSetter(prefService, key, value, String.class);
    }

    public static void commitLocalState(ClassLoader loader) {
        try {
            Reflect.callStatic(loader, BraveSymbols.BRAVE_LOCAL_STATE, "commitPendingWrite");
        } catch (Throwable ignored) {
            // Chromium's LocalStatePrefs bridge persists PrefService writes asynchronously.
        }
    }

    private static void invokePreferenceSetter(
            Object prefService, String key, Object value, Class<?> valueType) throws Exception {
        for (Method method : prefService.getClass().getDeclaredMethods()) {
            if (method.getReturnType() != void.class || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            int keyIndex;
            int valueIndex;
            if (parameters[0] == String.class && parameters[1] == valueType) {
                keyIndex = 0;
                valueIndex = 1;
            } else if (parameters[0] == valueType && parameters[1] == String.class) {
                keyIndex = 1;
                valueIndex = 0;
            } else {
                continue;
            }
            Object[] arguments = new Object[2];
            arguments[keyIndex] = key;
            arguments[valueIndex] = value;
            method.setAccessible(true);
            method.invoke(prefService, arguments);
            return;
        }
        throw new NoSuchMethodException(
                prefService.getClass().getName() + " preference setter for " + valueType.getName());
    }

    static Object selectProfilePrefService(List<Object> services, Object localState) {
        for (int i = services.size() - 1; i >= 0; i--) {
            Object service = services.get(i);
            if (service != null && service != localState) {
                return service;
            }
        }
        return null;
    }

    private static Object capturedProfilePrefs(ClassLoader loader, Object localState) {
        List<Object> liveServices = new ArrayList<>();
        synchronized (PREF_SERVICES) {
            List<WeakReference<Object>> services = PREF_SERVICES.get(loader);
            if (services == null) {
                return null;
            }
            for (int i = services.size() - 1; i >= 0; i--) {
                Object service = services.get(i).get();
                if (service == null) {
                    services.remove(i);
                } else {
                    liveServices.add(0, service);
                }
            }
        }
        return selectProfilePrefService(liveServices, localState);
    }

    private static Object recordedProfile(ClassLoader loader) {
        synchronized (REGULAR_PROFILES) {
            WeakReference<Object> reference = REGULAR_PROFILES.get(loader);
            return reference == null ? null : reference.get();
        }
    }

    private static final class BrowserNotReadyException extends IllegalStateException {
        BrowserNotReadyException(String message) {
            super(message);
        }
    }

    public static void setGlobalShieldValue(
            ClassLoader loader, Object profile, String resource, String value) throws Exception {
        if (profile == null) {
            throw new IllegalStateException("Regular profile is not ready");
        }
        Class<?> settings = Class.forName(
                BraveSymbols.SHIELDS_CONTENT_SETTINGS, false, loader);
        for (Method method : settings.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != void.class
                    || !isShieldValueSetter(method, profile)) {
                continue;
            }
            method.setAccessible(true);
            method.invoke(null, shieldArguments(method, profile, resource, value));
            return;
        }
        throw new NoSuchMethodException(settings.getName() + " shield value setter");
    }

    public static Context context(Object owner) throws Exception {
        for (Class<?> current = owner.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 0
                        || !Context.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object value = method.invoke(owner);
                    if (value instanceof Context context) {
                        return context;
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Try the next context accessor exposed by the fragment hierarchy.
                }
            }
        }
        throw new NoSuchMethodException(owner.getClass().getName() + " context accessor");
    }

    private static boolean isShieldValueSetter(Method method, Object profile) {
        int strings = 0;
        int booleans = 0;
        int profiles = 0;
        for (Class<?> parameter : method.getParameterTypes()) {
            if (parameter == String.class) {
                strings++;
            } else if (parameter == boolean.class) {
                booleans++;
            } else if (parameter.isInstance(profile)) {
                profiles++;
            } else {
                return false;
            }
        }
        return strings == 3 && profiles == 1 && booleans <= 1;
    }

    private static Object[] shieldArguments(
            Method method, Object profile, String resource, String value) {
        Object[] arguments = new Object[method.getParameterCount()];
        String[] strings = {"", resource, value};
        int stringIndex = 0;
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == String.class) {
                arguments[i] = strings[stringIndex++];
            } else if (parameters[i] == boolean.class) {
                arguments[i] = false;
            } else {
                arguments[i] = profile;
            }
        }
        return arguments;
    }
}
