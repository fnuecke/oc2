/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PortFilter {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PORT_COUNT = 65536;

    // --------------------------------------------------------------------- //

    private final boolean[] denied = new boolean[PORT_COUNT];
    private final boolean hasRules;

    // --------------------------------------------------------------------- //

    public PortFilter(final Collection<String> rules) {
        final List<String> rejected = new ArrayList<>();
        boolean any = false;
        for (final String rule : rules) {
            final String trimmed = rule.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            try {
                any |= apply(trimmed);
            } catch (final Exception e) {
                LOGGER.error("Unparsable internet port rule \"{}\": {}", trimmed, e.getMessage());
                rejected.add(trimmed);
            }
        }
        if (!rejected.isEmpty()) {
            // Same reasoning as the address deny list: a port rule that silently does nothing is dangerous.
            throw new IllegalArgumentException(
                "Internet port rules that cannot be enforced: " + rejected
                    + ". Fix or remove them; refusing to enable internet access with an incomplete filter.");
        }
        hasRules = any;
    }

    // --------------------------------------------------------------------- //

    public boolean isAllowed(final short port) {
        return !hasRules || !denied[Short.toUnsignedInt(port)];
    }

    @Override
    public String toString() {
        if (!hasRules) {
            return "[]";
        }
        final StringBuilder builder = new StringBuilder("[");
        int start = -1;
        for (int port = 0; port <= PORT_COUNT; ++port) {
            final boolean set = port < PORT_COUNT && denied[port];
            if (set && start < 0) {
                start = port;
            } else if (!set && start >= 0) {
                if (builder.length() > 1) {
                    builder.append(", ");
                }
                builder.append(start);
                if (port - 1 != start) {
                    builder.append('-').append(port - 1);
                }
                start = -1;
            }
        }
        return builder.append(']').toString();
    }

    // --------------------------------------------------------------------- //

    private boolean apply(final String rule) {
        final int dash = rule.indexOf('-');
        final int from;
        final int to;
        if (dash > 0) {
            from = parsePort(rule.substring(0, dash).trim());
            to = parsePort(rule.substring(dash + 1).trim());
        } else {
            from = to = parsePort(rule);
        }
        if (to < from) {
            throw new IllegalArgumentException("Range runs backwards: " + rule);
        }
        for (int port = from; port <= to; ++port) {
            denied[port] = true;
        }
        return true;
    }

    private static int parsePort(final String text) {
        final int port = Integer.parseInt(text);
        if (port < 0 || port >= PORT_COUNT) {
            throw new IllegalArgumentException("Port out of range: " + text);
        }
        return port;
    }
}
