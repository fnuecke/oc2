package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public final class InetUtils {
    private static int bufferChecksum(final ByteBuffer buffer, final int size) {
        final int halfSize = size >>> 1;
        int checksum = 0;
        for (int i = 0; i < halfSize; ++i) {
            checksum += Short.toUnsignedInt(buffer.getShort());
        }
        if ((size & 1) != 0) {
            checksum += (buffer.get() << 8) & 0xFFFF;
        }
        return checksum;
    }

    private static short finishChecksum(int checksum) {
        checksum = (checksum >>> 16) + (checksum & 0xFFFF);
        checksum = (checksum >>> 16) + (checksum & 0xFFFF);
        return (short) ~checksum;
    }

    public static short rfc1071Checksum(final ByteBuffer buffer, final int size) {
        final int checksum = bufferChecksum(buffer, size);
        return finishChecksum(checksum);
    }

    public static short rfc1071Checksum(final ByteBuffer buffer) {
        return rfc1071Checksum(buffer, buffer.remaining());
    }

    public static short transportRfc1071Checksum(
        final ByteBuffer buffer,
        final int srcIpAddress,
        final int dstIpAddress,
        final byte protocol
    ) {
        final int size = buffer.remaining();
        final int checksumPart = bufferChecksum(buffer, size);
        final int checksum = checksumPart + Byte.toUnsignedInt(protocol) + size +
            (srcIpAddress >>> 16) + (srcIpAddress & 0xFFFF) +
            (dstIpAddress >>> 16) + (dstIpAddress & 0xFFFF);
        return finishChecksum(checksum);
    }

    private static InetAddress getInetAddressByBytes(final byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            /* should not be there */
            throw new Error("unreachable", e);
        }
    }

    public static InetAddress toJavaInetAddress(final int ipAddress) {
        final byte[] bytes = new byte[]{
            (byte) (ipAddress >>> 24),
            (byte) (ipAddress >>> 16),
            (byte) (ipAddress >>> 8),
            (byte) (ipAddress)
        };
        return getInetAddressByBytes(bytes);
    }

    private static void fillLong(final byte[] destination, final int offset, final long value) {
        for (int position = 0; position < 8; ++position) {
            destination[offset + position] = (byte) (value >>> ((7 - position) << 3));
        }
    }

    public static InetAddress toJavaInetAddress(final long ipAddressMost, final long ipAddressLeast) {
        final byte[] bytes = new byte[16];
        fillLong(bytes, 0, ipAddressMost);
        fillLong(bytes, 8, ipAddressLeast);
        return getInetAddressByBytes(bytes);
    }

    public static void ipv4AddressToString(final StringBuilder builder, final int ipAddress) {
        builder.append(Integer.toUnsignedString(ipAddress >>> 24));
        builder.append('.');
        builder.append(Integer.toUnsignedString((ipAddress >>> 16) & 0xFF));
        builder.append('.');
        builder.append(Integer.toUnsignedString((ipAddress >>> 8) & 0xFF));
        builder.append('.');
        builder.append(Integer.toUnsignedString(ipAddress & 0xFF));
    }

    public static String ipv4AddressToString(final int ipAddress) {
        final StringBuilder stringBuilder = new StringBuilder();
        ipv4AddressToString(stringBuilder, ipAddress);
        return stringBuilder.toString();
    }

    private static char hexCodeToChar(final int code) {
        if (code < 10) {
            return (char) ('0' + code);
        } else {
            return (char) ('A' + (code - 10));
        }
    }

    private static void byteToHex(final StringBuilder builder, final byte code) {
        builder.append(hexCodeToChar(code >>> 4));
        builder.append(hexCodeToChar(code & 15));
    }

    public static void macAddressToString(final StringBuilder builder, final MacAddress macAddress) {
        final short prefix = macAddress.prefix();
        final int address = macAddress.address();
        byteToHex(builder, (byte) (prefix >>> 8));
        builder.append(':');
        byteToHex(builder, (byte) prefix);
        for (int i = 3; i >= 0; --i) {
            builder.append(':');
            byteToHex(builder, (byte) (address >>> (8 * i)));
        }
    }

    public static String macAddressToString(final MacAddress macAddress) {
        final StringBuilder builder = new StringBuilder();
        macAddressToString(builder, macAddress);
        return builder.toString();
    }

    public static void socketAddressToString(final StringBuilder builder, final int ipAddress, final short port) {
        ipv4AddressToString(builder, ipAddress);
        builder.append(':');
        builder.append(Short.toUnsignedInt(port));
    }

    public static byte[] quickICMPBody(final ByteBuffer data) {
        final int tmpPosition = data.position();
        final int tmpLimit = data.limit();
        data.limit(data.capacity());
        data.position(LinkLocalLayer.FRAME_HEADER_SIZE);
        final int headerSize = (data.get() & 0xF) * 4;
        data.position(LinkLocalLayer.FRAME_HEADER_SIZE);
        data.limit(LinkLocalLayer.FRAME_HEADER_SIZE + headerSize + 8);
        final byte[] result = new byte[data.remaining() + 4];
        result[2] = 0x5;
        result[3] = (byte) 0xDC;
        data.put(result, 4, data.remaining());
        data.limit(tmpLimit);
        data.position(tmpPosition);
        return result;
    }

    public static int javaInetAddressToIpAddress(final Inet4Address address) {
        final byte[] bytes = address.getAddress();
        return (Byte.toUnsignedInt(bytes[0]) << 24) | (Byte.toUnsignedInt(bytes[1]) << 16)
            | (Byte.toUnsignedInt(bytes[2]) << 8) | Byte.toUnsignedInt(bytes[3]);
    }

    public static int indexOf(final CharSequence string, final char character, final int start) {
        for (int i = start, length = string.length(); i < length; ++i) {
            if (string.charAt(i) == character) {
                return i;
            }
        }
        return -1;
    }

    public static int surelyParseValidIpv4Address(final CharSequence string) {
        int position = 0;
        int address = 0;
        for (int i = 0; i < 3; ++i) {
            final int segmentEnd = indexOf(string, '.', position);
            address = (address << 8) | Integer.parseUnsignedInt(string, position, segmentEnd, 10);
            position = segmentEnd + 1;
        }
        return (address << 8) | Integer.parseUnsignedInt(string, position, string.length(), 10);
    }

    public static int parseIpv4Address(final CharSequence string) throws AddressParseException {
        if (!Ipv4Space.ipAddressPattern.matcher(string).matches()) {
            throw new AddressParseException("Not an IPv4 address: " + string);
        }
        return surelyParseValidIpv4Address(string);
    }

    private static int hexCodeToInt(final char code) throws AddressParseException {
        if (code >= '0' && code <= '9') {
            return code - '0';
        } else if (code >= 'a' && code <= 'f') {
            return code - 'a' + 10;
        } else if (code >= 'A' && code <= 'F') {
            return code - 'A' + 10;
        } else {
            throw new AddressParseException("Illegal character '" + code + "' in address");
        }
    }

    private static byte parseMacAddressByte(final CharSequence string, final int start) throws AddressParseException {
        return (byte) ((hexCodeToInt(string.charAt(start)) << 4) | hexCodeToInt(string.charAt(start + 1)));
    }

    private static AddressParseException illegalDelimiter(final CharSequence string, final int index) {
        final char illegal = string.charAt(index);
        return new AddressParseException("Illegal character '" + illegal + "' at index " + index + " in MAC address \"" + string + "\"");
    }

    public static MacAddress parseMacAddress(final CharSequence string) throws AddressParseException {
        if (string.length() != 17) {
            throw new AddressParseException("MAC address length must be 17 characters: \"" + string + "\"");
        }
        final byte first = parseMacAddressByte(string, 0);
        if (string.charAt(2) != ':') {
            throw illegalDelimiter(string, 2);
        }
        final short prefix = (short) (first << 8 | parseMacAddressByte(string, 3));
        int address = 0;
        for (int i = 0; i < 4; ++i) {
            final int pos = i * 3 + 5;
            if (string.charAt(pos) != ':') {
                throw illegalDelimiter(string, pos);
            }
            address = (address << 8) | parseMacAddressByte(string, pos + 1);
        }
        return new MacAddress(prefix, address);
    }

    public static int getSubnetByPrefix(final int prefix) {
        if (prefix > 30 || prefix < 0) {
            throw new IllegalArgumentException("Wrong subnet prefix range");
        }
        return -1 << (32 - prefix);
    }

    private static void configureIpSpace(final Ipv4Space ipSpace, final String hosts) {
        int i = 1;
        for (final String hostString : hosts.split(",")) {
            final String rangeString = hostString.trim();
            if (rangeString.isEmpty()) {
                continue;
            }
            try {
                ipSpace.put(rangeString);
            } catch (final Exception e) {
                throw new IllegalArgumentException("Failed to parse IPv4 address range #" + i + ": " + e.getMessage());
            }
            ++i;
        }
    }

    public static Ipv4Space computeIpSpace(final String deniedHosts, final String allowedHosts) {
        final boolean deniedHostsIsEmpty = deniedHosts.trim().isEmpty();
        final boolean allowedHostsIsEmpty = allowedHosts.trim().isEmpty();
        if (deniedHostsIsEmpty && allowedHostsIsEmpty) {
            return new Ipv4Space(Ipv4Space.Modes.DENYLIST);
        } else if (allowedHostsIsEmpty) {
            final Ipv4Space ipSpace = new Ipv4Space(Ipv4Space.Modes.DENYLIST);
            configureIpSpace(ipSpace, deniedHosts);
            return ipSpace;
        } else if (deniedHostsIsEmpty) {
            final Ipv4Space ipSpace = new Ipv4Space(Ipv4Space.Modes.ALLOWLIST);
            configureIpSpace(ipSpace, allowedHosts);
            return ipSpace;
        } else {
            throw new IllegalArgumentException("Both denied and allowed hosts are specified");
        }
    }

    public static <PL, CL> PL createLayerIfNotStub(final CL currentLayer, final Function<CL, PL> getNextLayer) {
        if (currentLayer == NullLayer.INSTANCE) {
            // noinspection unchecked
            return (PL) NullLayer.INSTANCE;
        } else {
            return getNextLayer.apply(currentLayer);
        }
    }

    public static LayerParameters nextLayerParameters(final LayerParameters layerParameters, final String layerName) {
        final Optional<Tag> nextLayerState = layerParameters.getSavedState()
            .flatMap(currentLayerState -> (currentLayerState instanceof CompoundTag tag) ?
                Optional.ofNullable(tag.get(layerName)) :
                Optional.empty());
        return new LayerParametersImpl(nextLayerState, layerParameters.getInternetManager());
    }
}
