/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReflectOps {
    private static final String FILE_PREFIX = "@";
    private static final String OUT_PREFIX = "out=";

    // --------------------------------------------------------------------- //

    public static Results.Outcome run(final String op, final String rest, final Object root, final ReflectContext ctx) {
        final List<String> tokens = new ArrayList<>(tokenize(rest));

        Path out = null;
        if (!tokens.isEmpty() && tokens.getLast().startsWith(OUT_PREFIX)) {
            out = Path.of(tokens.removeLast().substring(OUT_PREFIX.length()));
        }

        final ReflectContext joining = new ReflectContext(
            ctx.registries, ctx.entities, ctx.maxDepth, ReflectOps::join);

        final Results.Outcome outcome = switch (op) {
            case "list" ->
                Results.Outcome.success(Reflect.list(root, tokens.isEmpty() ? null : tokens.getFirst(), joining));
            case "get" -> Results.Outcome.success(Reflect.get(root, require(tokens, 0, "path"), joining));
            case "set" -> Results.Outcome.success(
                Reflect.set(root, require(tokens, 0, "path"), value(require(tokens, 1, "value")), joining));
            case "invoke" -> Results.Outcome.success(
                Reflect.invoke(root, require(tokens, 0, "path"), values(tokens, 1), joining));
            case "items" -> Items.run(root, tokens, joining);
            default -> throw new IllegalArgumentException("unknown op " + op);
        };

        if (out == null) {
            return outcome;
        }

        final String body = Results.render(outcome);
        try {
            Files.writeString(out, body, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Results.Outcome(outcome.result(),
            new Results.Written(out.toAbsolutePath().toString(), body.length()));
    }

    static List<String> tokenize(final String text) {
        final StringReader reader = new StringReader(text);
        final List<String> tokens = new ArrayList<>();
        try {
            while (true) {
                reader.skipWhitespace();
                if (!reader.canRead()) {
                    return tokens;
                }

                final int start = reader.getCursor();
                final char next = reader.peek();
                if (StringReader.isQuotedStringStart(next)) {
                    reader.readQuotedString();
                } else if (next == '{' || next == '[') {
                    skipNesting(reader);
                } else if (next == '}' || next == ']') {
                    throw new IllegalArgumentException("unbalanced brackets in: " + text);
                } else {
                    while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                        reader.skip();
                    }
                }
                tokens.add(text.substring(start, reader.getCursor()));
            }
        } catch (final CommandSyntaxException e) {
            throw new IllegalArgumentException(e.getMessage() + " in: " + text);
        }
    }

    private static void skipNesting(final StringReader reader) throws CommandSyntaxException {
        int depth = 0;
        while (reader.canRead()) {
            final char c = reader.read();
            if (StringReader.isQuotedStringStart(c)) {
                reader.setCursor(reader.getCursor() - 1);
                reader.readQuotedString();
            } else if (c == '{' || c == '[') {
                depth++;
            } else if ((c == '}' || c == ']') && --depth == 0) {
                return;
            }
        }
        throw new IllegalArgumentException("unbalanced brackets in: " + reader.getString());
    }

    // --------------------------------------------------------------------- //

    private static void join(final Object value) {
        if (value instanceof final AbstractVirtualMachine vm) {
            if (vm.runner != null) {
                vm.runner.join();
            }
            return;
        }

        for (final Field field : FieldUtils.getAllFieldsList(value.getClass())) {
            if (Modifier.isStatic(field.getModifiers())
                || !AbstractVirtualMachine.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                final AbstractVirtualMachine vm = (AbstractVirtualMachine) field.get(value);
                if (vm != null && vm.runner != null) {
                    vm.runner.join();
                }
            } catch (final Throwable ignored) {
            }
        }
    }

    private static String require(final List<String> tokens, final int index, final String what) {
        if (index >= tokens.size()) {
            throw new IllegalArgumentException("missing " + what);
        }
        return tokens.get(index);
    }

    private static List<String> values(final List<String> tokens, final int from) {
        final List<String> result = new ArrayList<>();
        for (int i = from; i < tokens.size(); i++) {
            result.add(value(tokens.get(i)));
        }
        return result;
    }

    private static String value(final String token) {
        if (!token.startsWith(FILE_PREFIX)) {
            return token;
        }
        try {
            return Files.readString(Path.of(token.substring(FILE_PREFIX.length())), StandardCharsets.UTF_8).trim();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ReflectOps() {
    }
}
