/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.doc;

import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.common.doc.DeviceDocumentation.IOMethod;
import li.cil.oc2.common.doc.DeviceDocumentation.RPCMethod;
import li.cil.oc2.common.doc.DeviceDocumentation.RPCParameter;

import java.util.*;
import java.util.stream.Collectors;

public final class DevicePage {
    public static String pageName(final Class<?> type) {
        return Callbacks.getTypeNames(type).getFirst();
    }

    public static String title(final String pageName) {
        return Arrays.stream(pageName.split("_"))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }

    public static List<String> page(final Class<?> type, final DeviceDocumentation device) {
        final ArrayList<String> lines = new ArrayList<>();
        lines.add("# " + title(pageName(type)));

        final DeviceDocumentation.RPC rpc = device.rpc();
        if (rpc != null) {
            lines.add("");
            lines.add("## High-level API");
            lines.add("Device name: " + rpc.typeNames().stream()
                .map(name -> "`" + name + "`").collect(Collectors.joining(", ")));
            rpc.description().ifPresent(description -> {
                lines.add("");
                addParagraphs(lines, description);
            });
            lines.add("");
            lines.add("### Methods");
            final Map<String, List<RPCMethod>> overloadsByName = rpc.methods().stream()
                .collect(Collectors.groupingBy(RPCMethod::name, TreeMap::new, Collectors.toList()));
            overloadsByName.values().stream()
                .flatMap(overloads -> overloads.stream()
                    .peek(method -> requireOptionalOverloads(type, method, overloads))
                    .filter(method -> !isCoveredByOverload(method, overloads))
                    .sorted(Comparator.comparingInt(method -> method.parameters().size())))
                .forEach(method -> {
                    lines.add("");
                    addMethod(lines, method);
                });
        }

        final DeviceDocumentation.IO io = device.io();
        if (io != null) {
            lines.add("");
            lines.add("## Mid-level API");
            lines.add("Device name: `" + io.name() + "`");
            io.description().ifPresent(description -> {
                lines.add("");
                addParagraphs(lines, description);
            });
            lines.add("");
            lines.add("### Methods");
            io.methods().stream()
                .sorted(Comparator.comparingInt(IOMethod::code))
                .forEach(method -> {
                    lines.add("");
                    addMethod(lines, method);
                });
        }

        return lines;
    }

    public static List<String> index(final Collection<String> pageNames) {
        final ArrayList<String> lines = new ArrayList<>();
        lines.add("# Devices");
        lines.add("This is the reference for the devices computers can control. Every device listed here offers its methods through the [high-level API](../hlapi.md) to Linux; some also offer them through the [mid-level API](../mlapi.md) to CP/M. Each page names the device as a guest sees it, and lists its methods with their parameters and results.");
        lines.add("");
        lines.add("These pages are generated from the devices themselves, so they always match the installed version.");
        lines.add("");
        pageNames.stream().sorted().forEach(pageName ->
            lines.add("- [" + title(pageName) + "](" + pageName + ".md)"));
        return lines;
    }

    // --------------------------------------------------------------------- //

    private static void requireOptionalOverloads(final Class<?> type, final RPCMethod method, final Collection<RPCMethod> overloads) {
        final List<RPCParameter> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (!parameters.get(i).optional()) {
                continue;
            }
            final int required = i;
            final boolean hasOverload = overloads.stream().anyMatch(other ->
                other.parameters().size() == required && sameLeadingTypes(other.parameters(), parameters, required));
            if (!hasOverload) {
                throw new IllegalArgumentException("Parameter [" + parameterName(parameters.get(i), i) + "] of [" +
                    type.getName() + "#" + method.name() + "] is optional, but no overload without it exists.");
            }
        }
    }

    private static boolean isCoveredByOverload(final RPCMethod method, final Collection<RPCMethod> overloads) {
        final List<RPCParameter> parameters = method.parameters();
        return overloads.stream().anyMatch(other -> {
            final List<RPCParameter> otherParameters = other.parameters();
            return otherParameters.size() > parameters.size()
                && sameLeadingTypes(otherParameters, parameters, parameters.size())
                && otherParameters.subList(parameters.size(), otherParameters.size()).stream().allMatch(RPCParameter::optional);
        });
    }

    private static boolean sameLeadingTypes(final List<RPCParameter> a, final List<RPCParameter> b, final int count) {
        for (int i = 0; i < count; i++) {
            if (!a.get(i).type().equals(b.get(i).type())) {
                return false;
            }
        }
        return true;
    }

    private static void addMethod(final List<String> lines, final RPCMethod method) {
        final StringBuilder signature = new StringBuilder();
        signature.append('`').append(method.name()).append('(');
        final List<RPCParameter> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            final RPCParameter parameter = parameters.get(i);
            if (i > 0) {
                signature.append(", ");
            }
            final String declaration = parameterName(parameter, i) + ":" + luaType(parameter.type());
            signature.append(parameter.optional() ? "[" + declaration + "]" : declaration);
        }
        signature.append(')');
        final String returnType = luaType(method.returnType());
        if (returnType != null) {
            signature.append(':').append(returnType);
        }
        signature.append('`');

        addSignature(lines, signature.toString(), method.description());
        for (int i = 0; i < parameters.size(); i++) {
            final RPCParameter parameter = parameters.get(i);
            final String name = parameterName(parameter, i);
            parameter.description().ifPresent(description -> lines.add("- `" + name + "`: " + description));
        }
        method.returnValueDescription().ifPresent(description -> lines.add("- Returns " + description));
    }

    private static void addMethod(final List<String> lines, final IOMethod method) {
        addSignature(lines, "`" + method.code() + " " + method.name() + "`", method.description());
        method.argumentsDescription().ifPresent(description -> lines.add("- Takes " + description));
        method.resultsDescription().ifPresent(description -> lines.add("- Returns " + description));
    }

    private static void addSignature(final List<String> lines, final String signature, final Optional<String> description) {
        lines.add(signature);
        description.ifPresent(text -> addParagraphs(lines, text));
    }

    private static void addParagraphs(final List<String> lines, final String text) {
        lines.addAll(Arrays.asList(text.strip().split("\n")));
    }

    private static String parameterName(final RPCParameter parameter, final int index) {
        return parameter.name().orElse("arg" + index);
    }

    private static String luaType(final Class<?> type) {
        if (type == void.class || type == Void.class) {
            return null;
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return "number";
        }
        if (CharSequence.class.isAssignableFrom(type) || type.isEnum() || type == byte[].class) {
            return "string";
        }
        return "table";
    }

    private DevicePage() {
    }
}
