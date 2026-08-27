/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.List;

public final class Results {
    public interface Payload {
    }

    // --------------------------------------------------------------------- //

    public record Value(@Nullable JsonElement value) implements Payload {
    }

    public record Listing(String type, List<Member> fields, List<Method> methods) implements Payload {
    }

    public record Member(String name, String type, boolean isStatic, @Nullable String preview) {
    }

    public record Method(String name, List<String> parameters, String returns) {
    }

    public record Target(@Nullable String target, JsonElement response) {
    }

    public record Targets(List<Target> targets) implements Payload {
    }

    public record Slot(int slot, @Nullable String item, int count) {
    }

    public record Slots(List<Handler> handlers) implements Payload {
    }

    public record Handler(String name, List<Slot> slots) {
    }

    public record Inserted(String item, int requested, int inserted, int remainder, String handler) implements Payload {
    }

    public record Extracted(String item, int requested, int extracted, @Nullable String handler) implements Payload {
    }

    public record Rejected(String item, int requested, List<String> candidates) implements Payload {
    }

    public record Written(String file, int bytes) implements Payload {
    }

    public record Dispatched(int clients, String note) implements Payload {
    }

    public record Failure(String error, String type) implements Payload {
    }

    public record Screenshot(@Nullable String name, String message) implements Payload {
    }

    // --------------------------------------------------------------------- //

    public record Outcome(String result, Payload payload) {
        public static Outcome success(final Payload payload) {
            return new Outcome("success", payload);
        }

        public static Outcome error(final Payload payload) {
            return new Outcome("error", payload);
        }

        public static Outcome error(final Throwable e) {
            final Throwable cause = e.getCause() != null && e instanceof RuntimeException ? e.getCause() : e;
            return error(new Failure(String.valueOf(cause.getMessage()), cause.getClass().getSimpleName()));
        }
    }

    // --------------------------------------------------------------------- //

    private static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    // --------------------------------------------------------------------- //

    public static JsonObject tree(final Outcome outcome) {
        final JsonObject result = new JsonObject();
        result.addProperty("result", outcome.result());
        for (final var entry : GSON.toJsonTree(outcome.payload()).getAsJsonObject().entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static String render(final Outcome outcome) {
        return GSON.toJson(tree(outcome));
    }

    // --------------------------------------------------------------------- //

    private Results() {
    }
}
