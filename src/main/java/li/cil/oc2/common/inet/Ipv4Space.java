package li.cil.oc2.common.inet;

import li.cil.oc2.common.util.IntegerSpace;

import javax.annotation.Nullable;
import javax.annotation.RegEx;
import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Ipv4Space extends IntegerSpace {

    private static final String IPADDRESS_PATTERN =
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}";
    private static final Pattern ipAddressPattern =
            line(group("ip", IPADDRESS_PATTERN));
    private static final Pattern ipRangePattern =
            line(group("start", IPADDRESS_PATTERN) + "-" + group("end", IPADDRESS_PATTERN));
    private static final Pattern subnetPattern =
            line(group("ip", IPADDRESS_PATTERN) + "\\/" + group("prefix", "[1-9]\\d?"));
    private static final Pattern interfaceNamePattern =
            line("@" + group("name", "[a-zA-Z].*"));
    private static final Pattern interfaceIdPattern =
            line("@" + group("id", "\\d*"));
    private final boolean isAllowListMode;

    public Ipv4Space(final Modes mode) {
        isAllowListMode = mode == Modes.ALLOWLIST;
    }

    private static Pattern line(@RegEx final String pattern) {
        return Pattern.compile('^' + pattern + '$');
    }

    private static String group(final String name, @RegEx final String pattern) {
        return "(?<" + name + ">" + pattern + ")";
    }

    @Override
    protected void elementToString(final StringBuilder builder, final int element) {
        InetUtils.ipAddressToString(builder, element);
    }

    private boolean putSubnet(final int ipAddress, final int prefix) {
        final int subnet = InetUtils.getSubnetByPrefix(prefix);
        final int rangeStart = ipAddress & subnet;
        final int rangeEnd = ipAddress | ~subnet;
        return put(rangeStart, rangeEnd);
    }

    private boolean putNetworkInterface(@Nullable final NetworkInterface networkInterface) {
        if (networkInterface == null) {
            throw new IllegalArgumentException("Network interface not found");
        }
        boolean result = false;
        for (final InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            final InetAddress inetAddress = address.getAddress();
            if (inetAddress instanceof Inet4Address) {
                final int ipAddress = InetUtils.javaInetAddressToIpAddress((Inet4Address) inetAddress);
                result = putSubnet(ipAddress, address.getNetworkPrefixLength()) || result;
            }
        }
        return result;
    }

    public boolean put(final String string) {
        final Matcher ipAddressMatch = ipAddressPattern.matcher(string);
        if (ipAddressMatch.matches()) {
            final int ipAddress = InetUtils.parseIpv4Address(ipAddressMatch.group("ip"));
            return put(ipAddress);
        }

        final Matcher ipRangeMatch = ipRangePattern.matcher(string);
        if (ipRangeMatch.matches()) {
            final int rangeStart = InetUtils.parseIpv4Address(ipRangeMatch.group("start"));
            final int rangeEnd = InetUtils.parseIpv4Address(ipRangeMatch.group("end"));
            return put(rangeStart, rangeEnd);
        }

        final Matcher subnetMatch = subnetPattern.matcher(string);
        if (subnetMatch.matches()) {
            final int ipAddress = InetUtils.parseIpv4Address(subnetMatch.group("ip"));
            final int prefix = Integer.parseInt(subnetMatch.group("prefix"));
            return putSubnet(ipAddress, prefix);
        }

        final Matcher interfaceNameMatch = interfaceNamePattern.matcher(string);
        if (interfaceNameMatch.matches()) {
            final String interfaceName = interfaceNameMatch.group("name");
            try {
                final NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
                return putNetworkInterface(networkInterface);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to get a network interface by name");
            }
        }

        final Matcher interfaceIdMatch = interfaceIdPattern.matcher(string);
        if (interfaceIdMatch.matches()) {
            final int interfaceId = Integer.parseInt(interfaceIdMatch.group("id"));
            try {
                final NetworkInterface networkInterface = NetworkInterface.getByIndex(interfaceId);
                return putNetworkInterface(networkInterface);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to get a network interface by index");
            }
        }

        // Assume it is a hostname
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(string);
            boolean result = false;
            for (final InetAddress address : addresses) {
                if (address instanceof Inet4Address) {
                    final int ipAddress = InetUtils.javaInetAddressToIpAddress((Inet4Address) address);
                    result = put(ipAddress) || result;
                }
            }
            return result;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public boolean isAllowed(final int ipAddress) {
        return isAllowListMode == contains(ipAddress);
    }

    public enum Modes {
        ALLOWLIST,
        DENYLIST,
    }
}
