package moe.brave.prune.hook;

import moe.brave.prune.compat.BraveRuntime;
import moe.brave.prune.prefs.ProductPreferenceOverrides;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class PrefServiceHooks {
    private static final String PREF_SERVICE = "org.chromium.components.prefs.PrefService";

    private PrefServiceHooks() {}

    public static int install(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) throws Exception {
        Class<?> prefService = Class.forName(PREF_SERVICE, false, loader);
        List<Method> factories = matching(
                prefService, method -> isFactory(method, prefService));
        List<Method> booleanSetters = matching(prefService, PrefServiceHooks::isBooleanSetter);
        List<Method> integerGetters = matching(prefService, PrefServiceHooks::isIntegerGetter);
        List<Method> integerSetters = matching(prefService, PrefServiceHooks::isIntegerSetter);
        List<Method> stringSetters = matching(prefService, PrefServiceHooks::isStringSetter);
        requireUnique(prefService, "native factory", factories);
        requireUnique(prefService, "boolean setter", booleanSetters);
        requireUnique(prefService, "integer getter", integerGetters);
        requireUnique(prefService, "integer setter", integerSetters);
        requireUnique(prefService, "string setter", stringSetters);

        int installed = 0;
        Method factory = factories.get(0);
        factory.setAccessible(true);
        handles.add(module.hook(factory).intercept(chain -> {
            Object prefServiceInstance = chain.proceed();
            BraveRuntime.recordPrefService(loader, prefServiceInstance);
            return prefServiceInstance;
        }));
        installed++;

        Method booleanSetter = booleanSetters.get(0);
        booleanSetter.setAccessible(true);
        handles.add(module.hook(booleanSetter).intercept(chain -> {
            int keyIndex = parameterIndex(booleanSetter, String.class);
            int valueIndex = parameterIndex(booleanSetter, boolean.class);
            String key = (String) chain.getArg(keyIndex);
            Boolean value = forcedBoolean(key);
            if (value != null) {
                Object[] arguments = {chain.getArg(0), chain.getArg(1)};
                arguments[valueIndex] = value;
                return chain.proceed(arguments);
            }
            return chain.proceed();
        }));
        installed++;

        Method integerGetter = integerGetters.get(0);
        integerGetter.setAccessible(true);
        handles.add(module.hook(integerGetter).intercept(chain -> {
            Integer value = forcedInteger((String) chain.getArg(0));
            return value != null ? value : chain.proceed();
        }));
        installed++;

        Method integerSetter = integerSetters.get(0);
        integerSetter.setAccessible(true);
        handles.add(module.hook(integerSetter).intercept(chain -> {
            int keyIndex = parameterIndex(integerSetter, String.class);
            int valueIndex = parameterIndex(integerSetter, int.class);
            String key = (String) chain.getArg(keyIndex);
            Integer value = forcedInteger(key);
            if (value != null) {
                Object[] arguments = {chain.getArg(0), chain.getArg(1)};
                arguments[valueIndex] = value;
                return chain.proceed(arguments);
            }
            return chain.proceed();
        }));
        installed++;
        return installed;
    }

    static boolean isBooleanSetter(Method method) {
        return method.getReturnType() == void.class
                && hasParameters(method, String.class, boolean.class);
    }

    static boolean isFactory(Method method, Class<?> prefService) {
        return Modifier.isStatic(method.getModifiers())
                && method.getReturnType() == prefService
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == long.class;
    }

    static boolean isIntegerGetter(Method method) {
        return method.getReturnType() == int.class
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == String.class;
    }

    static boolean isIntegerSetter(Method method) {
        return method.getReturnType() == void.class
                && hasParameters(method, String.class, int.class);
    }

    static boolean isStringSetter(Method method) {
        return method.getReturnType() == void.class
                && method.getParameterCount() == 2
                && method.getParameterTypes()[0] == String.class
                && method.getParameterTypes()[1] == String.class;
    }

    static Integer forcedInteger(String key) {
        return ProductPreferenceOverrides.integerValue(key);
    }

    static Boolean forcedBoolean(String key) {
        return ProductPreferenceOverrides.booleanValue(key);
    }

    private static boolean hasParameters(Method method, Class<?> first, Class<?> second) {
        if (method.getParameterCount() != 2) {
            return false;
        }
        Class<?>[] parameters = method.getParameterTypes();
        return (parameters[0] == first && parameters[1] == second)
                || (parameters[0] == second && parameters[1] == first);
    }

    private static List<Method> matching(Class<?> type, MethodMatcher matcher) {
        List<Method> methods = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            if (matcher.matches(method)) {
                methods.add(method);
            }
        }
        return methods;
    }

    private static void requireUnique(
            Class<?> type, String description, List<Method> methods)
            throws NoSuchMethodException {
        if (methods.size() != 1) {
            throw new NoSuchMethodException(
                    type.getName() + " " + description + " count=" + methods.size());
        }
    }

    private static int parameterIndex(Method method, Class<?> type) {
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == type) {
                return i;
            }
        }
        throw new IllegalArgumentException("Missing parameter " + type.getName());
    }

    @FunctionalInterface
    private interface MethodMatcher {
        boolean matches(Method method);
    }
}
