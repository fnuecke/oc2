/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l3;

import li.cil.oc2.common.inet.util.AddressParseException;
import li.cil.oc2.common.inet.util.InternetUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AddressFilter {
    private static final Logger LOGGER = LogManager.getLogger();

    // Nothing a guest could legitimately reach; denied regardless of configuration.
    private static final List<String> BUILT_IN_DENIED = List.of(
        "0.0.0.0/8",         // "This" network; connecting to it lands on loopback.
        "127.0.0.0/8",       // Loopback.
        "169.254.169.254",   // Cloud instance metadata, which hands out the host's cloud credentials.
        "224.0.0.0/4",       // Multicast, which the OS would send onto the local segment.
        "240.0.0.0/4"        // Reserved, including the limited broadcast.
    );

    // --------------------------------------------------------------------- //

    private final Ipv4Space staticAllowed = new Ipv4Space();
    private final Ipv4Space staticDenied = new Ipv4Space();
    private final boolean denyLocalInterfaceSubnets;
    private final List<String> allowedHostNames = new ArrayList<>();
    private final List<String> deniedHostNames = new ArrayList<>();
    private final boolean hasAllowRules;
    private volatile Ipv4Space resolvedAllowed = new Ipv4Space();
    private volatile Ipv4Space resolvedDenied = new Ipv4Space();
    private volatile Ipv4Space localDenied = new Ipv4Space();
    private final Map<String, List<Integer>> lastResolved = new ConcurrentHashMap<>();

    // --------------------------------------------------------------------- //

    public AddressFilter(
        final Collection<String> allowedRules,
        final Collection<String> deniedRules,
        final boolean denyLocalInterfaceSubnets
    ) {
        this.denyLocalInterfaceSubnets = denyLocalInterfaceSubnets;

        // An allow rule that cannot be parsed only ever makes the filter stricter, so it is logged
        // and skipped. A deny rule that cannot be parsed makes it weaker, so it is fatal.
        applyRules(allowedRules, staticAllowed, allowedHostNames, "allow");
        final List<String> rejected = applyRules(deniedRules, staticDenied, deniedHostNames, "deny");
        if (!rejected.isEmpty()) {
            throw new IllegalArgumentException(
                "Internet deny rules that cannot be enforced: " + rejected
                    + ". Fix or remove them; refusing to enable internet access with an incomplete filter.");
        }

        hasAllowRules = countMeaningfulRules(allowedRules) > 0;
        if (hasAllowRules && staticAllowed.isEmpty() && allowedHostNames.isEmpty()) {
            LOGGER.error("Every entry in the internet allow list was rejected, so nothing is "
                + "reachable. Fix the entries listed above.");
        }

        refresh();

        for (final String hostName : deniedHostNames) {
            if (lastResolved.get(hostName) == null) {
                throw new IllegalArgumentException(
                    "Internet deny rule host \"" + hostName + "\" does not resolve. "
                        + "Refusing to enable internet access with an unenforceable deny rule.");
            }
        }
    }

    // --------------------------------------------------------------------- //

    public static AddressFilter withBuiltInDenials(
        final Collection<String> allowedRules,
        final Collection<String> deniedRules,
        final boolean denyLocalInterfaceSubnets
    ) {
        final List<String> denied = new ArrayList<>(BUILT_IN_DENIED);
        denied.addAll(deniedRules);
        return new AddressFilter(allowedRules, denied, denyLocalInterfaceSubnets);
    }

    public boolean isAllowed(final int ipAddress) {
        final long address = InternetUtils.toUnsigned(ipAddress);
        if (staticDenied.contains(address) || resolvedDenied.contains(address)
            || localDenied.contains(address)) {
            return false;
        }
        if (!hasAllowRules) {
            return true;
        }
        return staticAllowed.contains(address) || resolvedAllowed.contains(address);
    }

    public void refresh() {
        // Deny sets first, could momentarily widen access via allow unintentionally otherwise.
        resolvedDenied = resolveAll(deniedHostNames);
        if (denyLocalInterfaceSubnets) {
            localDenied = enumerateLocalSubnets();
        }
        resolvedAllowed = resolveAll(allowedHostNames);
    }

    public boolean needsPeriodicRefresh() {
        return !allowedHostNames.isEmpty() || !deniedHostNames.isEmpty();
    }

    @Override
    public String toString() {
        return "AddressFilter{allowed=" + staticAllowed + ", denied=" + staticDenied
            + ", allowedHosts=" + allowedHostNames + ", deniedHosts=" + deniedHostNames + "}";
    }

    // --------------------------------------------------------------------- //

    private static List<String> applyRules(
        final Collection<String> rules,
        final Ipv4Space space,
        final List<String> hostNames,
        final String kind
    ) {
        final List<String> rejected = new ArrayList<>();
        for (final String rule : rules) {
            final String trimmed = rule.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            try {
                if (!applyRule(trimmed, space)) {
                    hostNames.add(trimmed);
                }
            } catch (final Exception e) {
                LOGGER.error("Unparsable internet {} rule \"{}\": {}", kind, trimmed, e.getMessage());
                rejected.add(trimmed);
            }
        }
        return rejected;
    }

    private static boolean applyRule(final String rule, final Ipv4Space space) throws Exception {
        if (rule.startsWith("@")) {
            addNetworkInterface(rule.substring(1), space);
            return true;
        }

        final int dash = rule.indexOf('-');
        if (dash > 0) {
            // Only a range if both halves really are addresses. Host names contain hyphens far more
            // often than ranges appear, and treating one as a range would silently drop the rule.
            final String low = rule.substring(0, dash).trim();
            final String high = rule.substring(dash + 1).trim();
            if (isDottedQuad(low) && isDottedQuad(high)) {
                space.add(InternetUtils.toUnsigned(InternetUtils.parseIpv4Address(low)),
                    InternetUtils.toUnsigned(InternetUtils.parseIpv4Address(high)));
                return true;
            }
        }

        final int slash = rule.indexOf('/');
        if (slash > 0) {
            final long address = InternetUtils.toUnsigned(InternetUtils.parseIpv4Address(rule.substring(0, slash).trim()));
            final int prefix = Integer.parseInt(rule.substring(slash + 1).trim());
            space.addSubnet(address, prefix);
            return true;
        }

        if (isDottedQuad(rule)) {
            space.add(InternetUtils.toUnsigned(InternetUtils.parseIpv4Address(rule)));
            return true;
        }

        return false;
    }

    private static boolean isDottedQuad(final String rule) {
        try {
            InternetUtils.parseIpv4Address(rule);
            return true;
        } catch (final AddressParseException e) {
            return false;
        }
    }

    private static void addNetworkInterface(final String selector, final Ipv4Space space) throws SocketException {
        final NetworkInterface networkInterface = selector.chars().allMatch(Character::isDigit)
            ? NetworkInterface.getByIndex(Integer.parseInt(selector))
            : NetworkInterface.getByName(selector);
        if (networkInterface == null) {
            throw new IllegalArgumentException("No such network interface: " + selector);
        }
        addInterfaceSubnets(networkInterface, space);
    }

    private static void addInterfaceSubnets(final NetworkInterface networkInterface, final Ipv4Space space) {
        for (final InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            if (address.getAddress() instanceof final Inet4Address inet4Address) {
                final int prefix = address.getNetworkPrefixLength();
                if (prefix < 0 || prefix > 32) {
                    // Some platforms report no prefix at all for an interface.
                    continue;
                }
                space.addSubnet(
                    InternetUtils.toUnsigned(InternetUtils.javaInetAddressToIpAddress(inet4Address)), prefix);
            }
        }
    }

    private static Ipv4Space enumerateLocalSubnets() {
        final Ipv4Space space = new Ipv4Space();
        try {
            for (final NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                addInterfaceSubnets(networkInterface, space);
            }
        } catch (final SocketException e) {
            // Failing open here would leave the operator's own network reachable while the log line
            // saying so scrolls away, so treat it as a reason not to run at all.
            throw new IllegalStateException(
                "Could not enumerate local network interfaces, so the subnets this server sits on "
                    + "cannot be blocked. Refusing to enable internet access.", e);
        }
        return space;
    }

    private Ipv4Space resolveAll(final List<String> hostNames) {
        final Ipv4Space space = new Ipv4Space();
        for (final String hostName : hostNames) {
            for (final int address : resolve(hostName)) {
                space.add(InternetUtils.toUnsigned(address));
            }
        }
        return space;
    }

    private List<Integer> resolve(final String hostName) {
        try {
            final List<Integer> addresses = new ArrayList<>();
            for (final InetAddress address : InetAddress.getAllByName(hostName)) {
                if (address instanceof final Inet4Address inet4Address) {
                    addresses.add(InternetUtils.javaInetAddressToIpAddress(inet4Address));
                }
            }
            lastResolved.put(hostName, addresses);
            return addresses;
        } catch (final UnknownHostException e) {
            final List<Integer> previous = lastResolved.get(hostName);
            if (previous != null) {
                // Keeping the stale answer is the safe direction for a deny rule: a name that fails
                // to resolve must not become reachable just because DNS hiccuped.
                LOGGER.warn("Could not resolve internet rule host \"{}\"; keeping its last known addresses.",
                    hostName);
                return previous;
            }
            LOGGER.warn("Could not resolve internet rule host \"{}\"; it matches nothing until it resolves.",
                hostName);
            return List.of();
        }
    }

    private static int countMeaningfulRules(final Collection<String> rules) {
        int count = 0;
        for (final String rule : rules) {
            final String trimmed = rule.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                ++count;
            }
        }
        return count;
    }
}
