/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public final class InternetUtils {
    public static short rfc1071Checksum(final ByteBuffer buffer, final int size) {
        return foldChecksum(sumWords(buffer, size));
    }

    public static short rfc1071Checksum(final ByteBuffer buffer) {
        return rfc1071Checksum(buffer, buffer.remaining());
    }

    public static short transportRfc1071Checksum(
        final ByteBuffer buffer,
        final int sourceIpAddress,
        final int destinationIpAddress,
        final byte protocol
    ) {
        final int size = buffer.remaining();
        int checksum = sumWords(buffer, size);
        checksum += Byte.toUnsignedInt(protocol);
        checksum += size;
        checksum += (sourceIpAddress >>> 16) & 0xFFFF;
        checksum += sourceIpAddress & 0xFFFF;
        checksum += (destinationIpAddress >>> 16) & 0xFFFF;
        checksum += destinationIpAddress & 0xFFFF;
        return foldChecksum(checksum);
    }

    public static InetAddress toJavaInetAddress(final int ipAddress) {
        final byte[] bytes = {
            (byte) (ipAddress >>> 24),
            (byte) (ipAddress >>> 16),
            (byte) (ipAddress >>> 8),
            (byte) ipAddress,
        };
        try {
            return InetAddress.getByAddress(bytes);
        } catch (final UnknownHostException e) {
            throw new IllegalStateException("Four bytes is always a valid address.", e);
        }
    }

    public static int javaInetAddressToIpAddress(final Inet4Address address) {
        final byte[] bytes = address.getAddress();
        return (Byte.toUnsignedInt(bytes[0]) << 24)
            | (Byte.toUnsignedInt(bytes[1]) << 16)
            | (Byte.toUnsignedInt(bytes[2]) << 8)
            | Byte.toUnsignedInt(bytes[3]);
    }

    public static long toUnsigned(final int ipAddress) {
        return Integer.toUnsignedLong(ipAddress);
    }

    public static void ipv4AddressToString(final StringBuilder builder, final int ipAddress) {
        builder.append((ipAddress >>> 24) & 0xFF).append('.')
            .append((ipAddress >>> 16) & 0xFF).append('.')
            .append((ipAddress >>> 8) & 0xFF).append('.')
            .append(ipAddress & 0xFF);
    }

    public static String ipv4AddressToString(final int ipAddress) {
        final StringBuilder builder = new StringBuilder();
        ipv4AddressToString(builder, ipAddress);
        return builder.toString();
    }

    public static void socketAddressToString(final StringBuilder builder, final int ipAddress, final short port) {
        ipv4AddressToString(builder, ipAddress);
        builder.append(':').append(Short.toUnsignedInt(port));
    }

    public static int parseIpv4Address(final String string) throws AddressParseException {
        final String[] parts = string.split("\\.", -1);
        if (parts.length != 4) {
            throw new AddressParseException("Not an IPv4 address: " + string);
        }
        int address = 0;
        for (final String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                throw new AddressParseException("Not an IPv4 address: " + string);
            }
            final int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (final NumberFormatException e) {
                throw new AddressParseException("Not an IPv4 address: " + string);
            }
            if (octet < 0 || octet > 255) {
                throw new AddressParseException("Octet out of range in: " + string);
            }
            address = (address << 8) | octet;
        }
        return address;
    }

    // --------------------------------------------------------------------- //

    /**
     * Sums {@code size} bytes starting at the buffer's position as 16-bit big-endian words, leaving
     * the position past them. An odd trailing byte is padded with a zero, per RFC 1071.
     */
    private static int sumWords(final ByteBuffer buffer, final int size) {
        int checksum = 0;
        final int wordCount = size >>> 1;
        for (int i = 0; i < wordCount; ++i) {
            checksum += Short.toUnsignedInt(buffer.getShort());
        }
        if ((size & 1) != 0) {
            checksum += Byte.toUnsignedInt(buffer.get()) << 8;
        }
        return checksum;
    }

    private static short foldChecksum(int checksum) {
        while ((checksum >>> 16) != 0) {
            checksum = (checksum >>> 16) + (checksum & 0xFFFF);
        }
        return (short) ~checksum;
    }

    // --------------------------------------------------------------------- //

    private InternetUtils() {
    }
}
