package moe.brave.prune.settings;

import android.content.Context;
import android.content.SharedPreferences;

import moe.brave.prune.compat.Reflect;
import moe.brave.prune.prefs.BravePreferenceAdapter;
import moe.brave.prune.prefs.ModulePreferences;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class SettingsInjector {
    private static final String LANGUAGE_KEY = "brave_prune_language_fingerprinting";
    private static final Map<Object, Boolean> INJECTED = new WeakHashMap<>();

    private final ClassLoader loader;
    private final BravePreferenceAdapter preferenceAdapter;

    public SettingsInjector(ClassLoader loader, BravePreferenceAdapter preferenceAdapter) {
        this.loader = loader;
        this.preferenceAdapter = preferenceAdapter;
    }

    public void inject(Object fragment, Context context) throws Exception {
        synchronized (INJECTED) {
            if (INJECTED.containsKey(fragment)) {
                return;
            }
        }

        Class<?> screenType = Class.forName(
                "androidx.preference.PreferenceScreen", false, loader);
        Object screen = invokeNoArgReturning(fragment, screenType);
        if (screen == null) {
            throw new IllegalStateException("PreferenceScreen is not ready");
        }

        Class<?> preferenceType = Class.forName(
                "androidx.preference.Preference", false, loader);
        Class<?> switchType = Reflect.findClass(
                loader,
                "org.chromium.components.browser_ui.settings.ChromeSwitchPreference",
                "androidx.preference.SwitchPreferenceCompat",
                "androidx.preference.SwitchPreference");
        SharedPreferences preferences = ModulePreferences.from(context);
        Object languageSwitch = createSwitch(
                switchType,
                preferenceType,
                context,
                preferences,
                LANGUAGE_KEY,
                languageTitle(),
                ModulePreferences.KEY_LANGUAGE_FINGERPRINTING,
                preferenceAdapter::applyLanguageProtection);
        addPreference(screen, languageSwitch, preferenceType, LANGUAGE_KEY);
        synchronized (INJECTED) {
            INJECTED.put(fragment, true);
        }
    }

    private static Object createSwitch(
            Class<?> switchType,
            Class<?> preferenceType,
            Context context,
            SharedPreferences preferences,
            String key,
            CharSequence title,
            String preferenceKey,
            BooleanChangeHandler handler) throws Exception {
        Object preference = Reflect.construct(switchType, context);
        invokeVoid(preference, String.class, key);
        setTitle(preference, title);
        setChecked(preference, preferences.getBoolean(preferenceKey, false));

        ListenerBinding listenerBinding = listenerBinding(preference, preferenceType);
        Class<?> listenerType = listenerBinding.listenerType();
        Object listener = Proxy.newProxyInstance(
                listenerType.getClassLoader(),
                new Class<?>[]{listenerType},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "BravePrunePreferenceListener[" + key + "]";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == (arguments == null || arguments.length == 0
                                    ? null
                                    : arguments[0]);
                            default -> null;
                        };
                    }
                    if (method.getReturnType() != boolean.class
                            || arguments == null
                            || arguments.length < 2) {
                        return false;
                    }
                    boolean enabled = Boolean.TRUE.equals(arguments[1]);
                    preferences.edit()
                            .putBoolean(preferenceKey, enabled)
                            .commit();
                    handler.apply(enabled);
                    return true;
        });
        listenerBinding.bind(preference, listener);
        return preference;
    }

    private static Object invokeNoArgReturning(Object receiver, Class<?> returnType)
            throws Exception {
        for (Class<?> current = receiver.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0
                        && returnType.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return method.invoke(receiver);
                }
            }
        }
        throw new NoSuchMethodException(receiver.getClass().getName() + " screen getter");
    }

    private static void invokeVoid(Object receiver, Class<?> parameterType, Object argument)
            throws Exception {
        for (Class<?> current = receiver.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())
                        && method.getReturnType() == void.class
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == parameterType) {
                    method.setAccessible(true);
                    method.invoke(receiver, argument);
                    return;
                }
            }
        }
        throw new NoSuchMethodException(receiver.getClass().getName());
    }

    private static void setTitle(Object preference, CharSequence title) throws Exception {
        List<Method> candidates = new ArrayList<>();
        for (Class<?> current = preference.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getReturnType() == void.class
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == CharSequence.class) {
                    method.setAccessible(true);
                    if (method.getName().equals("setTitle")) {
                        method.invoke(preference, title);
                        return;
                    }
                    candidates.add(method);
                }
            }
        }
        candidates.sort(Comparator.comparing(Method::getName));
        if (candidates.isEmpty()) {
            throw new NoSuchMethodException(preference.getClass().getName() + " title setter");
        }
        candidates.get(candidates.size() - 1).invoke(preference, title);
    }

    private static void setChecked(Object preference, boolean checked) throws Exception {
        for (Class<?> current = preference.getClass(); current != null;
                current = current.getSuperclass()) {
            List<Method> candidates = new ArrayList<>();
            for (Method method : current.getDeclaredMethods()) {
                if (method.getReturnType() == void.class
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == boolean.class) {
                    method.setAccessible(true);
                    if (method.getName().equals("setChecked")) {
                        method.invoke(preference, checked);
                        return;
                    }
                    candidates.add(method);
                }
            }
            if (candidates.size() == 1) {
                candidates.get(0).invoke(preference, checked);
                return;
            }
        }
        throw new NoSuchMethodException(preference.getClass().getName() + " checked setter");
    }

    static ListenerBinding listenerBinding(Object preference, Class<?> preferenceType)
            throws NoSuchMethodException {
        for (Class<?> current = preference.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getReturnType() != void.class || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> listener = method.getParameterTypes()[0];
                if (changeCallback(listener, preferenceType) != null) {
                    method.setAccessible(true);
                    return ListenerBinding.forSetter(listener, method);
                }
            }
        }

        // R8 can inline setOnPreferenceChangeListener() and leave only the listener field.
        for (Class<?> current = preference.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isFinal(field.getModifiers())
                        || changeCallback(field.getType(), preferenceType) == null) {
                    continue;
                }
                field.setAccessible(true);
                return ListenerBinding.forField(field.getType(), field);
            }
        }
        throw new NoSuchMethodException(preference.getClass().getName() + " change listener");
    }

    private static Method changeCallback(Class<?> listener, Class<?> preferenceType) {
        if (!listener.isInterface()) {
            return null;
        }
        for (Method callback : listener.getMethods()) {
            Class<?>[] parameters = callback.getParameterTypes();
            if (callback.getReturnType() == boolean.class
                    && parameters.length == 2
                    && !parameters[1].isPrimitive()
                    && (preferenceType.isAssignableFrom(parameters[0])
                            || parameters[0].isAssignableFrom(preferenceType))) {
                return callback;
            }
        }
        return null;
    }

    static void addPreference(
            Object screen, Object preference, Class<?> preferenceType) throws Exception {
        addPreference(screen, preference, preferenceType, LANGUAGE_KEY);
    }

    static void addPreference(
            Object screen,
            Object preference,
            Class<?> preferenceType,
            CharSequence key) throws Exception {
        Method findPreference = findPreferenceMethod(screen, preferenceType);
        List<Method> mutations = new ArrayList<>();
        for (Class<?> current = screen.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if ((method.getReturnType() == boolean.class
                                || method.getReturnType() == void.class)
                        && preferenceType.isAssignableFrom(parameter)
                        && parameter.isAssignableFrom(preference.getClass())) {
                    method.setAccessible(true);
                    mutations.add(method);
                }
            }
        }
        if (findPreference == null) {
            throw new NoSuchMethodException(screen.getClass().getName() + " preference lookup");
        }
        mutations.sort(Comparator.comparing(Method::getName));
        InvocationTargetException lastFailure = null;
        for (Method mutation : mutations) {
            try {
                mutation.invoke(screen, preference);
                if (findPreference.invoke(screen, key) == preference) {
                    return;
                }
            } catch (InvocationTargetException error) {
                lastFailure = error;
            }
        }
        NoSuchMethodException error = new NoSuchMethodException(
                screen.getClass().getName() + " add preference");
        if (lastFailure != null) {
            error.initCause(lastFailure.getCause());
        }
        throw error;
    }

    private static Method findPreferenceMethod(Object screen, Class<?> preferenceType) {
        Method result = null;
        for (Class<?> current = screen.getClass(); current != null;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 1
                        && preferenceType.isAssignableFrom(method.getReturnType())
                        && CharSequence.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    result = method;
                }
            }
        }
        return result;
    }

    private static String languageTitle() {
        return Locale.getDefault().getLanguage().equals("zh")
                ? "语言指纹保护"
                : "Language fingerprinting protection";
    }

    @FunctionalInterface
    private interface BooleanChangeHandler {
        void apply(boolean enabled);
    }

    static final class ListenerBinding {
        private final Class<?> listenerType;
        private final Method setter;
        private final Field field;

        private ListenerBinding(Class<?> listenerType, Method setter, Field field) {
            this.listenerType = listenerType;
            this.setter = setter;
            this.field = field;
        }

        static ListenerBinding forSetter(Class<?> listenerType, Method setter) {
            return new ListenerBinding(listenerType, setter, null);
        }

        static ListenerBinding forField(Class<?> listenerType, Field field) {
            return new ListenerBinding(listenerType, null, field);
        }

        Class<?> listenerType() {
            return listenerType;
        }

        void bind(Object preference, Object listener) throws ReflectiveOperationException {
            if (setter != null) {
                setter.invoke(preference, listener);
            } else {
                field.set(preference, listener);
            }
        }
    }
}
