/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;


import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Reflect {
    private record Segment(String name, List<String> indices) {
    }

    private interface Target {
        @Nullable
        Object get();

        void set(@Nullable Object value);

        Class<?> type();

        @Nullable
        Type generic();
    }

    // --------------------------------------------------------------------- //

    public static Results.Value get(final Object root, final String path, final ReflectContext ctx) {
        return new Results.Value(Values.toJson(target(root, path, ctx).get(), ctx));
    }

    @Nullable
    public static Object resolve(final Object root, final String path, final ReflectContext ctx) {
        return target(root, path, ctx).get();
    }

    public static Results.Value set(final Object root, final String path, final String json, final ReflectContext ctx) {
        final Target target = target(root, path, ctx);
        target.set(Values.fromJson(json, target.type(), target.generic(), ctx));
        return new Results.Value(Values.toJson(target.get(), ctx));
    }

    public static Results.Value invoke(final Object root, final String path, final List<String> args, final ReflectContext ctx) {
        final List<Segment> segments = parse(path);
        final Object owner = walk(root, segments, segments.size() - 1, ctx);
        final Segment last = segments.get(segments.size() - 1);
        if (!last.indices().isEmpty()) {
            throw new IllegalArgumentException("a method name cannot be indexed: " + last.name());
        }

        final Method method = select(owner, last.name(), args, ctx);
        method.setAccessible(true);

        final Class<?>[] types = method.getParameterTypes();
        final Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = Values.fromJson(args.get(i), types[i], method.getGenericParameterTypes()[i], ctx);
        }

        try {
            final Object result = method.invoke(Modifier.isStatic(method.getModifiers()) ? null : owner, values);
            return new Results.Value(method.getReturnType() == void.class ? null : Values.toJson(result, ctx));
        } catch (final Throwable e) {
            throw unwrap(e);
        }
    }

    public static Results.Listing list(final Object root, @Nullable final String path, final ReflectContext ctx) {
        final Object value = path == null || path.isEmpty() ? root : target(root, path, ctx).get();
        if (value == null) {
            return new Results.Listing("null", List.of(), List.of());
        }

        final Class<?> type = value.getClass();

        final List<Results.Member> members = new ArrayList<>();
        for (final Field field : fields(type).values()) {
            final boolean isStatic = Modifier.isStatic(field.getModifiers());
            String preview;
            try {
                field.setAccessible(true);
                preview = preview(field.get(isStatic ? null : value));
            } catch (final Throwable e) {
                preview = null;
            }
            members.add(new Results.Member(field.getName(), field.getType().getSimpleName(), isStatic, preview));
        }

        final List<Results.Method> methods = new ArrayList<>();
        for (final Method method : methods(type)) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            methods.add(new Results.Method(method.getName(),
                Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toList(),
                method.getReturnType().getSimpleName()));
        }

        return new Results.Listing(type.getName(), members, methods);
    }

    // --------------------------------------------------------------------- //

    private static Target target(final Object root, final String path, final ReflectContext ctx) {
        final List<Segment> segments = parse(path);
        final Object owner = walk(root, segments, segments.size() - 1, ctx);
        final Segment last = segments.get(segments.size() - 1);
        final Field field = field(owner.getClass(), last.name());
        final boolean isStatic = Modifier.isStatic(field.getModifiers());
        final Object target = isStatic ? null : owner;

        if (last.indices().isEmpty()) {
            return new Target() {
                @Nullable
                @Override
                public Object get() {
                    try {
                        return field.get(target);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalArgumentException("cannot read " + field.getName() + ": " + e.getMessage());
                    }
                }

                @Override
                public void set(@Nullable final Object value) {
                    try {
                        field.set(target, value);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalArgumentException("cannot write " + field.getName()
                            + (field.getDeclaringClass().isRecord() ? " (record components are final)" : "")
                            + ": " + e.getMessage());
                    }
                }

                @Override
                public Class<?> type() {
                    return field.getType();
                }

                @Override
                public Type generic() {
                    return field.getGenericType();
                }
            };
        }

        Object container;
        try {
            container = field.get(target);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("cannot read " + field.getName() + ": " + e.getMessage());
        }
        final List<String> indices = last.indices();
        for (int i = 0; i < indices.size() - 1; i++) {
            container = index(container, indices.get(i), null, ctx);
        }
        return element(container, indices.get(indices.size() - 1), field.getGenericType(), indices.size(), ctx);
    }

    private static Object walk(final Object root, final List<Segment> segments, final int count, final ReflectContext ctx) {
        Object current = root;
        ctx.onVisit.accept(current);
        for (int i = 0; i < count; i++) {
            final Segment segment = segments.get(i);
            final Field field = field(current.getClass(), segment.name());
            Type generic = field.getGenericType();
            try {
                current = field.get(Modifier.isStatic(field.getModifiers()) ? null : current);
            } catch (final IllegalAccessException e) {
                throw new IllegalArgumentException("cannot read " + segment.name() + ": " + e.getMessage());
            }
            for (final String key : segment.indices()) {
                current = index(current, key, generic, ctx);
                generic = null;
            }
            if (current == null) {
                throw new IllegalArgumentException("null at " + segment.name());
            }
            ctx.onVisit.accept(current);
        }
        return current;
    }

    @Nullable
    private static Object index(@Nullable final Object container, final String key,
                                @Nullable final Type generic, final ReflectContext ctx) {
        if (container == null) {
            throw new IllegalArgumentException("cannot index null");
        }
        if (container.getClass().isArray()) {
            return Array.get(container, Integer.parseInt(key));
        }
        if (container instanceof final List<?> list) {
            return list.get(Integer.parseInt(key));
        }
        if (container instanceof final Map<?, ?> map) {
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (Objects.equals(String.valueOf(entry.getKey()), key)) {
                    return entry.getValue();
                }
            }
            final Object parsed = mapKey(key, generic, ctx);
            if (parsed != null && map.containsKey(parsed)) {
                return map.get(parsed);
            }
            throw new IllegalArgumentException("no key " + key + "; have " + map.keySet());
        }
        throw new IllegalArgumentException(container.getClass().getSimpleName() + " is not indexable");
    }

    @Nullable
    private static Object mapKey(final String key, @Nullable final Type generic, final ReflectContext ctx) {
        if (!(generic instanceof final ParameterizedType parameterized)) {
            return null;
        }
        final Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length != 2 || !(arguments[0] instanceof final Class<?> type)) {
            return null;
        }
        try {
            return Values.fromJson('"' + key + '"', type, null, ctx);
        } catch (final Throwable e) {
            return null;
        }
    }

    private static Target element(@Nullable final Object container, final String key,
                                  @Nullable final Type generic, final int depth, final ReflectContext ctx) {
        if (container == null) {
            throw new IllegalArgumentException("cannot index null");
        }

        final Class<?> component = container.getClass().isArray() ? container.getClass().getComponentType() : Object.class;
        final Type elementGeneric = depth == 1 && generic instanceof final ParameterizedType parameterized
            && parameterized.getActualTypeArguments().length > 0
            ? parameterized.getActualTypeArguments()[parameterized.getActualTypeArguments().length - 1]
            : null;

        return new Target() {
            @Nullable
            @Override
            public Object get() {
                return index(container, key, generic, ctx);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void set(@Nullable final Object value) {
                if (container.getClass().isArray()) {
                    Array.set(container, Integer.parseInt(key), value);
                } else if (container instanceof final List<?> list) {
                    ((List<Object>) list).set(Integer.parseInt(key), value);
                } else if (container instanceof final Map<?, ?> map) {
                    for (final Object existing : map.keySet()) {
                        if (Objects.equals(String.valueOf(existing), key)) {
                            ((Map<Object, Object>) map).put(existing, value);
                            return;
                        }
                    }
                    throw new IllegalArgumentException("no key " + key);
                } else {
                    throw new IllegalArgumentException(container.getClass().getSimpleName() + " is not indexable");
                }
            }

            @Override
            public Class<?> type() {
                if (component != Object.class) {
                    return component;
                }
                return elementGeneric instanceof final Class<?> c ? c : Object.class;
            }

            @Nullable
            @Override
            public Type generic() {
                return elementGeneric;
            }
        };
    }

    private static Method select(final Object owner, final String name,
                                 final List<String> args, final ReflectContext ctx) {
        final int signature = name.indexOf('(');
        final String plain = signature < 0 ? name : name.substring(0, signature);
        final List<String> wanted = signature < 0 ? null : Arrays.stream(
                name.substring(signature + 1, name.lastIndexOf(')')).split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();

        final List<Method> candidates = new ArrayList<>();
        for (final Method method : methods(owner.getClass())) {
            if (!method.getName().equals(plain) || method.getParameterCount() != args.size()) {
                continue;
            }
            if (wanted != null && !matches(method, wanted)) {
                continue;
            }
            candidates.add(method);
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("no method " + plain + " taking " + args.size()
                + " argument(s) on " + owner.getClass().getSimpleName() + "; try 'list'");
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        final List<Method> viable = new ArrayList<>();
        for (final Method method : candidates) {
            try {
                for (int i = 0; i < args.size(); i++) {
                    Values.fromJson(args.get(i), method.getParameterTypes()[i],
                        method.getGenericParameterTypes()[i], ctx);
                }
                viable.add(method);
            } catch (final Throwable ignored) {
            }
        }

        if (viable.isEmpty()) {
            throw new IllegalArgumentException("no overload of " + plain + " accepts those arguments; candidates: "
                + candidates.stream().map(m -> Arrays.toString(m.getParameterTypes())).toList());
        }
        if (viable.size() > 1) {
            throw new IllegalArgumentException("ambiguous call to " + plain + "; name the types, e.g. "
                + plain + "(" + String.join(",", Arrays.stream(viable.get(0).getParameterTypes())
                .map(Class::getSimpleName).toList()) + "). Candidates: "
                + viable.stream().map(m -> Arrays.toString(m.getParameterTypes())).toList());
        }
        return viable.get(0);
    }

    private static boolean matches(final Method method, final List<String> wanted) {
        final Class<?>[] types = method.getParameterTypes();
        if (types.length != wanted.size()) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            final String want = wanted.get(i);
            if (!types[i].getName().equals(want) && !types[i].getSimpleName().equals(want)) {
                return false;
            }
        }
        return true;
    }

    private static Field field(final Class<?> type, final String name) {
        final Field field = FieldUtils.getField(type, name, true);
        if (field == null) {
            throw new IllegalArgumentException("no field " + name + " on " + type.getName()
                + "; try 'list' to see what is there");
        }
        return field;
    }

    private static Map<String, Field> fields(final Class<?> type) {
        final Map<String, Field> result = new LinkedHashMap<>();
        for (final Field field : FieldUtils.getAllFieldsList(type)) {
            result.putIfAbsent(field.getName(), field);
        }
        return result;
    }

    private static List<Method> methods(final Class<?> type) {
        final List<Class<?>> hierarchy = new ArrayList<>();
        hierarchy.add(type);
        hierarchy.addAll(ClassUtils.getAllSuperclasses(type));
        hierarchy.addAll(ClassUtils.getAllInterfaces(type));

        final Map<String, Method> result = new LinkedHashMap<>();
        for (final Class<?> c : hierarchy) {
            for (final Method method : c.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    result.putIfAbsent(method.getName() + Arrays.toString(method.getParameterTypes()), method);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    private static String preview(@Nullable final Object value) {
        if (value == null) {
            return "null";
        }
        final String text = value.getClass().isArray()
            ? value.getClass().getComponentType().getSimpleName() + "[" + Array.getLength(value) + "]"
            : String.valueOf(value);
        return StringUtils.abbreviate(text, 60);
    }

    private static RuntimeException unwrap(final Throwable e) {
        final Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
        return cause instanceof final RuntimeException runtime
            ? runtime : new RuntimeException(cause.toString(), cause);
    }

    private static List<Segment> parse(final String path) {
        if (path.isEmpty() || path.endsWith(".")) {
            throw new IllegalArgumentException("empty path segment in '" + path + "'");
        }

        final List<Segment> segments = new ArrayList<>();
        for (final String part : path.split("\\.")) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("empty path segment in '" + path + "'");
            }
            final int bracket = part.indexOf('[');
            if (bracket < 0) {
                segments.add(new Segment(part, List.of()));
                continue;
            }
            final List<String> indices = new ArrayList<>();
            int i = bracket;
            while (i < part.length() && part.charAt(i) == '[') {
                final int end = part.indexOf(']', i);
                if (end < 0) {
                    throw new IllegalArgumentException("unclosed [ in '" + path + "'");
                }
                indices.add(part.substring(i + 1, end));
                i = end + 1;
            }
            if (i != part.length()) {
                throw new IllegalArgumentException("trailing '" + part.substring(i) + "' in '" + path + "'");
            }
            segments.add(new Segment(part.substring(0, bracket), indices));
        }
        return segments;
    }

    private Reflect() {
    }
}
