package moe.brave.prune.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class Reflect {
    private Reflect() {}

    public static Class<?> findClass(ClassLoader loader, String... names)
            throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String name : names) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last == null ? new ClassNotFoundException() : last;
    }

    public static List<Method> methods(Class<?> type, String name, int parameterCount) {
        List<Method> matches = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)
                        && method.getParameterCount() == parameterCount) {
                    method.setAccessible(true);
                    matches.add(method);
                }
            }
        }
        return matches;
    }

    public static Method method(
            Class<?> type, String name, boolean requireStatic, Object... arguments)
            throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method candidate : current.getDeclaredMethods()) {
                if (!candidate.getName().equals(name)
                        || candidate.getParameterCount() != arguments.length
                        || (requireStatic && !Modifier.isStatic(candidate.getModifiers()))) {
                    continue;
                }
                if (parametersAccept(candidate.getParameterTypes(), arguments)) {
                    candidate.setAccessible(true);
                    return candidate;
                }
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    public static Object call(Object receiver, String name, Object... arguments) throws Exception {
        return method(receiver.getClass(), name, false, arguments).invoke(receiver, arguments);
    }

    public static Object callStatic(
            ClassLoader loader, String className, String name, Object... arguments)
            throws Exception {
        Class<?> type = Class.forName(className, false, loader);
        return method(type, name, true, arguments).invoke(null, arguments);
    }

    public static Object construct(Class<?> type, Object... arguments) throws Exception {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == arguments.length
                    && parametersAccept(constructor.getParameterTypes(), arguments)) {
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            }
        }
        throw new NoSuchMethodException(type.getName() + " constructor");
    }

    public static Object readField(Object receiver, String name) throws Exception {
        for (Class<?> current = receiver.getClass(); current != null;
                current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(receiver);
            } catch (NoSuchFieldException ignored) {
                // Continue through compatibility superclasses.
            }
        }
        throw new NoSuchFieldException(receiver.getClass().getName() + "#" + name);
    }

    public static Object readStaticField(ClassLoader loader, String className, String name)
            throws Exception {
        Class<?> type = Class.forName(className, false, loader);
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static boolean parametersAccept(Class<?>[] parameters, Object[] arguments) {
        for (int i = 0; i < parameters.length; i++) {
            if (arguments[i] == null) {
                if (parameters[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> expected = wrap(parameters[i]);
            if (!expected.isAssignableFrom(arguments[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        return Void.class;
    }
}
