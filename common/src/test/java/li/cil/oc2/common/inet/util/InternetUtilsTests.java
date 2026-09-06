/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.util;

import li.cil.oc2.common.inet.l3.Ipv4Space;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InternetUtilsTests {
    private static ByteBuffer bytes(final int... values) {
        final ByteBuffer buffer = ByteBuffer.allocate(values.length);
        for (final int value : values) {
            buffer.put((byte) value);
        }
        buffer.flip();
        return buffer;
    }

    @Test
    public void headerChecksumMatchesTheWorkedExampleFromRfc1071() {
        // The IPv4 header from the RFC's example, with its checksum field zeroed.
        final ByteBuffer header = bytes(
            0x45, 0x00, 0x00, 0x73, 0x00, 0x00, 0x40, 0x00, 0x40, 0x11, 0x00, 0x00,
            0xc0, 0xa8, 0x00, 0x01, 0xc0, 0xa8, 0x00, 0xc7);

        assertEquals((short) 0xb861, InternetUtils.rfc1071Checksum(header));
    }

    @Test
    public void aChecksummedHeaderChecksumsToZero() {
        final ByteBuffer header = bytes(
            0x45, 0x00, 0x00, 0x73, 0x00, 0x00, 0x40, 0x00, 0x40, 0x11, 0xb8, 0x61,
            0xc0, 0xa8, 0x00, 0x01, 0xc0, 0xa8, 0x00, 0xc7);

        // Summing a message that already carries its checksum is the standard way to verify one.
        assertEquals((short) 0, InternetUtils.rfc1071Checksum(header));
    }

    @Test
    public void oddLengthMessagesArePaddedNotTruncated() {
        final ByteBuffer even = bytes(0x12, 0x34, 0x56, 0x00);
        final ByteBuffer odd = bytes(0x12, 0x34, 0x56);

        assertEquals(InternetUtils.rfc1071Checksum(even), InternetUtils.rfc1071Checksum(odd));
    }

    @Test
    public void transportChecksumVerifiesToZero() {
        final int source = 0xC0A80001;
        final int destination = 0x08080808;
        final byte protocol = 6;

        final ByteBuffer message = ByteBuffer.allocate(24);
        message.putShort((short) 40000); // Source port.
        message.putShort((short) 80); // Destination port.
        message.putInt(0x11223344); // Sequence.
        message.putInt(0x55667788); // Acknowledgment.
        message.put((byte) 0x50);
        message.put((byte) 0x10);
        message.putShort((short) 8192);
        message.putShort((short) 0); // Checksum.
        message.putShort((short) 0); // Urgent pointer.
        message.putInt(0xDEADBEEF); // Payload.
        message.flip();

        final short checksum = InternetUtils.transportRfc1071Checksum(message, source, destination, protocol);
        message.position(0);
        message.putShort(16, checksum);

        assertEquals((short) 0, InternetUtils.transportRfc1071Checksum(message, source, destination, protocol));
    }

    @Test
    public void addressesRoundTripThroughText() throws AddressParseException {
        for (final String address : new String[]{"0.0.0.0", "1.2.3.4", "127.0.0.1", "255.255.255.255", "169.254.169.254"}) {
            assertEquals(address, InternetUtils.ipv4AddressToString(InternetUtils.parseIpv4Address(address)));
        }
    }

    @Test
    public void addressesAboveTheSignedRangeParse() throws AddressParseException {
        assertEquals(0xFFFFFFFF, InternetUtils.parseIpv4Address("255.255.255.255"));
        assertEquals(0x80000000, InternetUtils.parseIpv4Address("128.0.0.0"));
        assertEquals(Ipv4Space.MAX_ADDRESS, InternetUtils.toUnsigned(InternetUtils.parseIpv4Address("255.255.255.255")));
    }

    @Test
    public void malformedAddressesAreRejected() {
        for (final String address : new String[]{"", "1.2.3", "1.2.3.4.5", "1.2.3.256", "1.2.3.-1", "a.b.c.d", "1.2.3.", "1..3.4"}) {
            assertThrows(AddressParseException.class, () -> InternetUtils.parseIpv4Address(address),
                "should have rejected \"" + address + "\"");
        }
    }
}
