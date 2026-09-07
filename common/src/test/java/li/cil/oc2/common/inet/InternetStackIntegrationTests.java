/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import li.cil.oc2.common.inet.l2.LinkLocalLayer;
import li.cil.oc2.common.inet.l3.AddressFilter;
import li.cil.oc2.common.inet.l3.NetworkLayer;
import li.cil.oc2.common.inet.l4.*;
import li.cil.oc2.common.inet.socket.ReachabilityProbe;
import li.cil.oc2.common.inet.socket.SocketManager;
import li.cil.oc2.common.inet.socket.SocketSessionLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"SameParameterValue", "BusyWait"})
public class InternetStackIntegrationTests {
    private static final short ETHERTYPE_ARP = 0x0806;
    private static final short ETHERTYPE_IPv4 = 0x0800;
    private static final byte PROTOCOL_ICMP = 1;
    private static final byte PROTOCOL_TCP = 6;
    private static final byte PROTOCOL_UDP = 17;

    private static final byte[] GUEST_MAC = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};
    private static final int GUEST_IP = 0x0A000002; // 10.0.0.2
    private static final byte[] OTHER_GUEST_MAC = {0x02, 0x00, 0x00, 0x00, 0x00, 0x02};
    private static final int OTHER_GUEST_IP = 0x0A000003; // 10.0.0.3
    private static final int GATEWAY_IP = 0x0A000001; // 10.0.0.1

    private ServerSocket server;
    private LinkLocalLayer stack;
    private SocketManager socketManager;

    private byte[] gatewayMac;
    private int guestSequence = 0x2000;
    private int gatewaySequence;

    // --------------------------------------------------------------------- //

    @BeforeEach
    public void setUp() throws IOException {
        server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        socketManager = new SocketManager();

        // Loopback is denied by the shipped defaults; this test needs it.
        final AddressFilter filter = new AddressFilter(List.of("127.0.0.0/8"), List.of(), false);
        stack = buildStack(filter, TimeUnit.SECONDS.toNanos(60), new SessionLimits(8, 32));
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (stack != null) {
            stack.onStop();
        }
        if (socketManager != null) {
            socketManager.close();
        }
        if (server != null) {
            server.close();
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void everyGuestOnTheSegmentGetsItsOwnArpReply() throws Exception {
        send(arpRequest(GUEST_MAC, GUEST_IP, GATEWAY_IP));
        send(arpRequest(OTHER_GUEST_MAC, OTHER_GUEST_IP, GATEWAY_IP));

        final List<byte[]> replies = new ArrayList<>();
        for (int i = 0; i < 200 && replies.size() < 2; ++i) {
            replies.addAll(pump());
            Thread.sleep(5);
        }

        assertEquals(2, replies.size(), "both guests should be answered");
        assertArrayEquals(GUEST_MAC, destinationMacOf(replies.get(0)),
            "the first reply should go to the first requester");
        assertArrayEquals(OTHER_GUEST_MAC, destinationMacOf(replies.get(1)),
            "the second reply should go to the second requester");
        assertEquals(GUEST_IP, arpTargetIpOf(replies.get(0)));
        assertEquals(OTHER_GUEST_IP, arpTargetIpOf(replies.get(1)));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void repliesGoToTheGuestThatOwnsTheAddress() throws Exception {
        resolveGateway();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(10000);
            final short peerPort = (short) peer.getLocalPort();

            send(udpFrame(GUEST_MAC, GUEST_IP, (short) 40001, peerPort, "ping".getBytes(StandardCharsets.UTF_8)));
            pump();

            final DatagramPacket incoming = new DatagramPacket(new byte[64], 64);
            peer.receive(incoming);

            // A second machine speaks after the first, so it is the most recently seen MAC.
            send(arpRequest(OTHER_GUEST_MAC, OTHER_GUEST_IP, GATEWAY_IP));
            pumpUntilEtherType(ETHERTYPE_ARP);

            final byte[] response = "pong".getBytes(StandardCharsets.UTF_8);
            peer.send(new DatagramPacket(response, response.length, incoming.getSocketAddress()));

            final byte[] reply = pumpUntilEtherType(ETHERTYPE_IPv4);
            assertArrayEquals(GUEST_MAC, destinationMacOf(reply),
                "the datagram belongs to the first guest and must not be addressed to the second");
            assertArrayEquals(response, parseUdp(reply), "the payload should still arrive intact");
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void anArpProbeIsNeitherAnsweredNorClaimed() throws Exception {
        // Sender protocol address 0.0.0.0, the shape RFC 5227 gives a probe.
        send(arpRequest(OTHER_GUEST_MAC, 0, GATEWAY_IP));

        for (int i = 0; i < 20; ++i) {
            assertTrue(pump().isEmpty(), "a probe must not be answered");
            Thread.sleep(5);
        }

        // The gateway must not have taken the probed address for itself, so a real request for it
        // still gets a reply.
        resolveGateway();
    }

    @Test
    @Timeout(30)
    public void gatewayAnswersArpForWhateverAddressTheGuestPicks() throws InterruptedException {
        resolveGateway();
    }

    @Test
    @Timeout(30)
    public void aGuestTcpConnectionReachesARealSocketAndBack() throws Exception {
        resolveGateway();

        final short port = (short) server.getLocalPort();

        // The guest opens the connection.
        send(tcpFrame(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, port, new byte[0]));

        final Tcp synAck = parseTcp(pumpUntilFrame());
        assertTrue(synAck.header().syn, "expected a SYN-ACK");
        assertTrue(synAck.header().ack);
        assertEquals(guestSequence + 1, synAck.header().acknowledgmentNumber);
        gatewaySequence = synAck.header().sequenceNumber + 1;
        guestSequence += 1;

        // The host socket really was accepted on the other side.
        final Socket accepted = server.accept();
        accepted.setSoTimeout(10000);

        send(tcpFrame(TcpHeader.FLAG_ACK, guestSequence, gatewaySequence, 8192, port, new byte[0]));

        // Guest to the world.
        final byte[] request = "GET / HTTP/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        send(tcpFrame(TcpHeader.FLAG_ACK | TcpHeader.FLAG_PSH, guestSequence, gatewaySequence, 8192, port, request));
        pump();

        final InputStream in = accepted.getInputStream();
        final byte[] received = new byte[request.length];
        int read = 0;
        while (read < request.length) {
            final int count = in.read(received, read, request.length - read);
            assertTrue(count > 0, "the socket closed before the request arrived");
            read += count;
        }
        assertArrayEquals(request, received, "what the guest sent should arrive byte for byte");

        // The world back to the guest.
        final byte[] response = "HTTP/1.0 200 OK\r\n\r\nhi".getBytes(StandardCharsets.UTF_8);
        final OutputStream out = accepted.getOutputStream();
        out.write(response);
        out.flush();

        guestSequence += request.length;

        final ByteBuffer collected = ByteBuffer.allocate(response.length);
        while (collected.hasRemaining()) {
            final Tcp segment = parseTcp(pumpUntilFrame());
            if (segment.payload().length == 0) {
                continue;
            }
            assertEquals(gatewaySequence, segment.header().sequenceNumber, "segments should arrive in order");
            collected.put(segment.payload());
            gatewaySequence += segment.payload().length;
            send(tcpFrame(TcpHeader.FLAG_ACK, guestSequence, gatewaySequence, 8192, port, new byte[0]));
        }
        assertArrayEquals(response, collected.array(), "what the world sent should arrive byte for byte");

        accepted.close();
    }

    @Test
    @Timeout(30)
    public void aGuestUdpExchangeReachesARealSocketAndBack() throws Exception {
        resolveGateway();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(10000);
            final short peerPort = (short) peer.getLocalPort();
            final short guestPort = (short) 40001;

            final byte[] request = "ping".getBytes(StandardCharsets.UTF_8);
            send(udpFrame(guestPort, peerPort, request));
            pump();

            final DatagramPacket incoming = new DatagramPacket(new byte[64], 64);
            peer.receive(incoming);
            assertArrayEquals(request,
                java.util.Arrays.copyOf(incoming.getData(), incoming.getLength()),
                "the datagram should arrive byte for byte");

            final byte[] response = "pong".getBytes(StandardCharsets.UTF_8);
            peer.send(new DatagramPacket(response, response.length, incoming.getSocketAddress()));

            assertArrayEquals(response, parseUdp(pumpUntilFrame()),
                "the reply should come back to the guest byte for byte");
        }
    }

    @Test
    @Timeout(30)
    public void aFrameAddressedToAnotherMachineIsIgnored() throws Exception {
        resolveGateway();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(200);
            send(udpFrame(OTHER_GUEST_MAC, GUEST_MAC, GUEST_IP, 0x7F000001,
                (short) 40004, (short) peer.getLocalPort(), "not for you".getBytes(StandardCharsets.UTF_8)));

            for (int i = 0; i < 20; ++i) {
                assertTrue(pump().isEmpty(), "a frame for another machine must not be answered");
                Thread.sleep(2);
            }
            assertThrows(SocketTimeoutException.class,
                () -> peer.receive(new DatagramPacket(new byte[64], 64)),
                "a frame for another machine must not be proxied");
        }
    }

    @Test
    @Timeout(30)
    public void aBroadcastIsNotAnsweredWithAnError() throws Exception {
        resolveGateway();

        send(udpFrame(new byte[]{-1, -1, -1, -1, -1, -1}, GUEST_MAC, GUEST_IP, 0xFFFFFFFF,
            (short) 68, (short) 67, "dhcp discover".getBytes(StandardCharsets.UTF_8)));

        for (int i = 0; i < 20; ++i) {
            assertTrue(pump().isEmpty(), "a broadcast must be dropped silently");
            Thread.sleep(2);
        }
    }

    @Test
    @Timeout(30)
    public void anIdleUdpSocketProducesNoFrames() throws Exception {
        resolveGateway();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            send(udpFrame((short) 40002, (short) peer.getLocalPort(),
                "open the socket".getBytes(StandardCharsets.UTF_8)));

            for (int i = 0; i < 20; ++i) {
                assertTrue(pump().isEmpty(), "an idle socket should produce no frames at all");
                Thread.sleep(2);
            }
        }
    }

    @Test
    @Timeout(30)
    public void anEmptyDatagramStillReachesTheGuest() throws Exception {
        resolveGateway();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(10000);
            final short peerPort = (short) peer.getLocalPort();

            send(udpFrame((short) 40003, peerPort, "open".getBytes(StandardCharsets.UTF_8)));
            pump();

            final DatagramPacket incoming = new DatagramPacket(new byte[64], 64);
            peer.receive(incoming);

            peer.send(new DatagramPacket(new byte[0], 0, incoming.getSocketAddress()));

            assertEquals(0, parseUdp(pumpUntilFrame()).length,
                "an empty datagram should arrive as an empty datagram, not be swallowed");
        }
    }

    @Test
    @Timeout(60)
    public void aHalfClosedConnectionStillExpires() throws Exception {
        final SessionLimits limits = new SessionLimits(4, 4);
        final AddressFilter filter = new AddressFilter(List.of("127.0.0.0/8"), List.of(), false);
        stack = buildStack(filter, TimeUnit.MILLISECONDS.toNanos(300), limits);

        resolveGateway();
        final short port = (short) server.getLocalPort();

        send(tcpFrame(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, port, new byte[0]));
        final Tcp synAck = parseTcp(pumpUntilFrame());
        assertTrue(synAck.header().syn);
        gatewaySequence = synAck.header().sequenceNumber + 1;
        guestSequence += 1;

        final Socket accepted = server.accept();
        send(tcpFrame(TcpHeader.FLAG_ACK, guestSequence, gatewaySequence, 8192, port, new byte[0]));
        pump();

        assertEquals(1, limits.getUsed(), "the connection should hold exactly one slot");

        // The far end goes away; the guest deliberately never closes its own half.
        accepted.close();

        for (int i = 0; i < 400 && limits.getUsed() > 0; ++i) {
            pump();
            Thread.sleep(5);
        }

        assertEquals(0, limits.getUsed(),
            "an idle half-closed connection must release its session slot");
    }

    @Test
    @Timeout(30)
    public void aResetGeneratedOnTeardownStillReachesTheGuest() throws Exception {
        resolveGateway();
        final short port = (short) server.getLocalPort();

        send(tcpFrame(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, port, new byte[0]));
        final Tcp synAck = parseTcp(pumpUntilFrame());
        gatewaySequence = synAck.header().sequenceNumber + 1;
        guestSequence += 1;

        final Socket accepted = server.accept();
        send(tcpFrame(TcpHeader.FLAG_ACK, guestSequence, gatewaySequence, 8192, port, new byte[0]));
        pump();

        // A SYN on a live connection means the guest lost its state, which must be answered with a
        // reset rather than silence.
        send(tcpFrame(TcpHeader.FLAG_SYN, guestSequence + 500, 0, 8192, port, new byte[0]));

        final Tcp reset = parseTcp(pumpUntilFrame());
        assertTrue(reset.header().rst, "the reset must actually reach the guest");

        accepted.close();
    }

    @Test
    @Timeout(30)
    public void aCancelledClaimDoesNotShiftTheNextReply() throws Exception {
        resolveGateway();
        final short port = (short) server.getLocalPort();

        // A UDP session that will sit idle, alongside a TCP session that has data to deliver.
        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            send(udpFrame((short) 40004, (short) peer.getLocalPort(),
                "open".getBytes(StandardCharsets.UTF_8)));
            pump();

            send(tcpFrame(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, port, new byte[0]));
            final Tcp synAck = parseTcp(pumpUntilFrame());
            gatewaySequence = synAck.header().sequenceNumber + 1;
            guestSequence += 1;

            final Socket accepted = server.accept();
            send(tcpFrame(TcpHeader.FLAG_ACK, guestSequence, gatewaySequence, 8192, port, new byte[0]));
            pump();

            final byte[] response = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
            accepted.getOutputStream().write(response);
            accepted.getOutputStream().flush();

            final ByteBuffer collected = ByteBuffer.allocate(response.length);
            while (collected.hasRemaining()) {
                final Tcp segment = parseTcp(pumpUntilFrame());
                if (segment.payload().length == 0) {
                    continue;
                }
                assertEquals(gatewaySequence, segment.header().sequenceNumber,
                    "the segment must be built at the start of the frame, not shifted");
                collected.put(segment.payload());
                gatewaySequence += segment.payload().length;
                send(tcpFrame(TcpHeader.FLAG_ACK, guestSequence, gatewaySequence, 8192, port, new byte[0]));
            }
            assertArrayEquals(response, collected.array());

            accepted.close();
        }
    }

    @Test
    @Timeout(30)
    public void aBlockedPortIsRefusedWithoutOpeningASocket() throws Exception {
        final short port = (short) server.getLocalPort();
        final AddressFilter filter = new AddressFilter(List.of("127.0.0.0/8"), List.of(), false);
        stack = buildStack(filter, new PortFilter(List.of(Integer.toString(Short.toUnsignedInt(port)))),
            TimeUnit.SECONDS.toNanos(60), new SessionLimits(8, 32));

        resolveGateway();
        send(tcpFrame(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, port, new byte[0]));

        final Tcp reply = parseTcp(pumpUntilFrame());
        assertTrue(reply.header().rst, "a closed port should be answered with a reset");
        assertFalse(reply.header().syn, "no handshake should be completed");

        // Nothing should have reached the listening socket.
        server.setSoTimeout(300);
        assertThrows(java.net.SocketTimeoutException.class, server::accept,
            "no connection should have been opened to the blocked port");
    }

    @Test
    @Timeout(30)
    public void aBlockedAddressIsRefusedWithoutOpeningASocket() throws Exception {
        resolveGateway();

        // A stack whose filter denies everything, standing in for the shipped defaults.
        final AddressFilter denyAll = new AddressFilter(List.of(), List.of("0.0.0.0/0"), false);
        final LinkLocalLayer blocked =
            buildStack(denyAll, TimeUnit.SECONDS.toNanos(60), new SessionLimits(8, 32));

        blocked.sendEthernetFrame(ByteBuffer.wrap(arpRequest(GUEST_IP, GATEWAY_IP)));
        final ByteBuffer buffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
        assertTrue(blocked.receiveEthernetFrame(buffer), "the ARP reply should still come back");
        buffer.get(6, gatewayMac); // This stack has a MAC of its own.

        final int portBefore = server.getLocalPort();
        blocked.sendEthernetFrame(ByteBuffer.wrap(
            tcpFrame(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, (short) portBefore, new byte[0])));

        // The refusal is an ICMP error, never a TCP handshake.
        buffer.clear();
        assertTrue(blocked.receiveEthernetFrame(buffer), "a blocked packet should be answered");
        final byte[] reply = new byte[buffer.remaining()];
        buffer.get(reply);

        final ByteBuffer parsed = ByteBuffer.wrap(reply);
        parsed.position(LinkLocalLayer.FRAME_HEADER_SIZE + 9);
        assertEquals(1, parsed.get(), "expected an ICMP packet");
        parsed.position(LinkLocalLayer.FRAME_HEADER_SIZE + 20);
        assertEquals(3, parsed.get(), "expected destination unreachable");
        assertEquals(13, parsed.get(), "expected administratively prohibited");

        blocked.onStop();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void anIcmpEchoRequestIsAnsweredWithAnEchoReply() throws Exception {
        resolveGateway();

        final byte[] payload = "abcdefgh".getBytes(StandardCharsets.UTF_8);
        send(icmpEchoRequest((short) 0x1234, (short) 7, payload));

        final byte[] reply = pumpUntilEtherType(ETHERTYPE_IPv4);

        final ByteBuffer buffer = ByteBuffer.wrap(reply);
        final int ipStart = LinkLocalLayer.FRAME_HEADER_SIZE;
        assertEquals(PROTOCOL_ICMP, buffer.get(ipStart + 9), "expected an ICMP packet");

        final int icmpStart = ipStart + (Byte.toUnsignedInt(buffer.get(ipStart)) & 0xF) * 4;
        assertEquals(0, buffer.get(icmpStart), "expected an echo reply");
        assertEquals(0, buffer.get(icmpStart + 1), "an echo reply carries code zero");
        assertEquals((short) 0x1234, buffer.getShort(icmpStart + 4), "the identifier should come back");
        assertEquals((short) 7, buffer.getShort(icmpStart + 6), "the sequence number should come back");

        final byte[] echoed = new byte[payload.length];
        buffer.position(icmpStart + 8);
        buffer.get(echoed);
        assertArrayEquals(payload, echoed, "the payload should come back unchanged");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void theGatewayReleasesAnAddressItClaimedBeforeItsOwnerSpokeUp() throws Exception {
        // Nobody has spoken yet, so the gateway cannot know the address is taken, and answers.
        send(arpRequest(OTHER_GUEST_MAC, OTHER_GUEST_IP, GUEST_IP));
        pumpUntilEtherType(ETHERTYPE_ARP);

        // Then the machine that owns it turns up.
        send(arpRequest(GUEST_MAC, GUEST_IP, GUEST_IP));
        pump();

        send(arpRequest(OTHER_GUEST_MAC, OTHER_GUEST_IP, GATEWAY_IP));
        assertEquals(GATEWAY_IP, arpSenderIpOf(pumpUntilEtherType(ETHERTYPE_ARP)),
            "the gateway should let go of an address that turned out to belong to a guest");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void theGatewayDoesNotClaimAnAddressAGuestHolds() throws Exception {
        // The first guest comes on the air, so its address is known to belong to a real machine.
        send(arpRequest(GUEST_MAC, GUEST_IP, GUEST_IP));
        pump();

        // The second guest looks the first one up, as it would for a DNS server on its segment.
        send(arpRequest(OTHER_GUEST_MAC, OTHER_GUEST_IP, GUEST_IP));
        for (final byte[] frame : pump()) {
            assertNotEquals(ETHERTYPE_ARP, ByteBuffer.wrap(frame).getShort(12),
                "the gateway must not answer for an address a guest holds");
        }

        // Having seen that, the gateway must still answer for its own address.
        send(arpRequest(OTHER_GUEST_MAC, OTHER_GUEST_IP, GATEWAY_IP));
        assertEquals(GATEWAY_IP, arpSenderIpOf(pumpUntilEtherType(ETHERTYPE_ARP)),
            "the gateway stopped answering for its own address");
    }

    // --------------------------------------------------------------------- //

    private LinkLocalLayer buildStack(final AddressFilter filter, final long timeoutNanos, final SessionLimits limits) {
        return buildStack(filter, new PortFilter(List.of()), timeoutNanos, limits);
    }

    private LinkLocalLayer buildStack(final AddressFilter filter, final PortFilter ports,
                                      final long timeoutNanos, final SessionLimits limits) {
        final SessionLayer sessionLayer =
            new SocketSessionLayer("test", socketManager, new ReachabilityProbe(Runnable::run, 100));
        final TransportLayer transportLayer = new TransportLayer(sessionLayer,
            ports, limits, new TokenBucket(1024, 1024), () -> 1,
            new StreamSession.TcpConfig(8192, 100, 1000), timeoutNanos);
        final NetworkLayer networkLayer = new NetworkLayer(transportLayer, filter);
        return new LinkLocalLayer(networkLayer);
    }

    private List<byte[]> pump() {
        return pump(stack);
    }

    private List<byte[]> pump(final LinkLocalLayer target) {
        socketManager.poll();
        target.onTick();

        final List<byte[]> frames = new ArrayList<>();
        final ByteBuffer buffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
        for (int i = 0; i < 16; ++i) {
            buffer.clear();
            if (!target.receiveEthernetFrame(buffer)) {
                break;
            }
            final byte[] frame = new byte[buffer.remaining()];
            buffer.get(frame);
            frames.add(frame);
        }
        return frames;
    }

    private byte[] pumpUntilFrame() throws InterruptedException {
        for (int i = 0; i < 200; ++i) {
            final List<byte[]> frames = pump();
            if (!frames.isEmpty()) {
                return frames.getFirst();
            }
            Thread.sleep(5);
        }
        throw new AssertionError("the stack produced no frame");
    }

    private byte[] pumpUntilEtherType(final short etherType) throws InterruptedException {
        for (int i = 0; i < 200; ++i) {
            for (final byte[] frame : pump()) {
                if (ByteBuffer.wrap(frame).getShort(12) == etherType) {
                    return frame;
                }
            }
            Thread.sleep(5);
        }
        throw new AssertionError("the stack produced no frame with EtherType " + etherType);
    }

    private void send(final byte[] frame) {
        stack.sendEthernetFrame(ByteBuffer.wrap(frame));
    }

    private static ByteBuffer frame(final byte[] destinationMac, final byte[] sourceMac, final short etherType, final int extra) {
        final ByteBuffer buffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_HEADER_SIZE + extra);
        buffer.put(destinationMac);
        buffer.put(sourceMac);
        buffer.putShort(etherType);
        return buffer;
    }

    private static byte[] arpRequest(final int senderIp, final int targetIp) {
        return arpRequest(GUEST_MAC, senderIp, targetIp);
    }

    private static byte[] arpRequest(final byte[] senderMac, final int senderIp, final int targetIp) {
        final ByteBuffer buffer = frame(new byte[]{-1, -1, -1, -1, -1, -1}, senderMac, ETHERTYPE_ARP, 28);
        buffer.putShort((short) 1); // Hardware type: Ethernet.
        buffer.putShort(ETHERTYPE_IPv4);
        buffer.put((byte) 6);
        buffer.put((byte) 4);
        buffer.putShort((short) 1); // Operation: request.
        buffer.put(senderMac);
        buffer.putInt(senderIp);
        buffer.put(new byte[6]);
        buffer.putInt(targetIp);
        return buffer.array();
    }

    private byte[] tcpFrame(final int flags, final int sequenceNumber, final int acknowledgmentNumber,
                            final int window, final short destinationPort, final byte[] payload) {
        final int tcpLength = 20 + payload.length;
        final int ipLength = 20 + tcpLength;
        final ByteBuffer buffer = frame(gatewayMac, GUEST_MAC, ETHERTYPE_IPv4, ipLength);
        final int ipStart = buffer.position();

        buffer.put((byte) 0x45);
        buffer.put((byte) 0);
        buffer.putShort((short) ipLength);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0x4000);
        buffer.put((byte) 64);
        buffer.put(PROTOCOL_TCP);
        buffer.putShort((short) 0); // Header checksum; the stack does not verify it.
        buffer.putInt(GUEST_IP);
        buffer.putInt(0x7F000001); // 127.0.0.1

        buffer.putShort((short) 40000);
        buffer.putShort(destinationPort);
        buffer.putInt(sequenceNumber);
        buffer.putInt(acknowledgmentNumber);
        buffer.put((byte) 0x50); // Data offset: five words.
        buffer.put((byte) flags);
        buffer.putShort((short) window);
        buffer.putShort((short) 0); // Checksum; the stack does not verify it.
        buffer.putShort((short) 0);
        buffer.put(payload);

        assertEquals(ipStart + ipLength, buffer.position());
        return buffer.array();
    }

    private byte[] icmpEchoRequest(final short identity, final short sequenceNumber, final byte[] payload) {
        final int icmpLength = 8 + payload.length;
        final int ipLength = 20 + icmpLength;
        final ByteBuffer buffer = frame(gatewayMac, GUEST_MAC, ETHERTYPE_IPv4, ipLength);

        buffer.put((byte) 0x45);
        buffer.put((byte) 0);
        buffer.putShort((short) ipLength);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0x4000);
        buffer.put((byte) 64);
        buffer.put(PROTOCOL_ICMP);
        buffer.putShort((short) 0); // Header checksum; the stack does not verify it.
        buffer.putInt(GUEST_IP);
        buffer.putInt(0x7F000001);

        buffer.put((byte) 8); // Echo request.
        buffer.put((byte) 0);
        buffer.putShort((short) 0); // Checksum; the stack does not verify it.
        buffer.putShort(identity);
        buffer.putShort(sequenceNumber);
        buffer.put(payload);
        return buffer.array();
    }

    private byte[] udpFrame(final short sourcePort, final short destinationPort, final byte[] payload) {
        return udpFrame(GUEST_MAC, GUEST_IP, sourcePort, destinationPort, payload);
    }

    private byte[] udpFrame(final byte[] sourceMac, final int sourceIp,
                            final short sourcePort, final short destinationPort, final byte[] payload) {
        return udpFrame(gatewayMac, sourceMac, sourceIp, 0x7F000001, sourcePort, destinationPort, payload);
    }

    private byte[] udpFrame(final byte[] destinationMac, final byte[] sourceMac, final int sourceIp, final int destinationIp,
                            final short sourcePort, final short destinationPort, final byte[] payload) {
        final int udpLength = 8 + payload.length;
        final int ipLength = 20 + udpLength;
        final ByteBuffer buffer = frame(destinationMac, sourceMac, ETHERTYPE_IPv4, ipLength);

        buffer.put((byte) 0x45);
        buffer.put((byte) 0);
        buffer.putShort((short) ipLength);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0x4000);
        buffer.put((byte) 64);
        buffer.put(PROTOCOL_UDP);
        buffer.putShort((short) 0); // Header checksum; the stack does not verify it.
        buffer.putInt(sourceIp);
        buffer.putInt(destinationIp);

        buffer.putShort(sourcePort);
        buffer.putShort(destinationPort);
        buffer.putShort((short) udpLength);
        buffer.putShort((short) 0); // Checksum; the stack does not verify it.
        buffer.put(payload);
        return buffer.array();
    }

    private byte[] parseUdp(final byte[] frame) {
        final ByteBuffer buffer = ByteBuffer.wrap(frame);
        buffer.position(12);
        assertEquals(ETHERTYPE_IPv4, buffer.getShort(), "expected an IPv4 frame");

        final int ipStart = buffer.position();
        final int headerSize = (Byte.toUnsignedInt(buffer.get(ipStart)) & 0xF) * 4;
        assertEquals(PROTOCOL_UDP, buffer.get(ipStart + 9), "expected a UDP packet");

        buffer.position(ipStart + headerSize);
        buffer.getShort(); // Source port.
        buffer.getShort(); // Destination port.
        final int length = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort(); // Checksum.

        final byte[] payload = new byte[length - 8];
        buffer.get(payload);
        return payload;
    }

    private record Tcp(TcpHeader header, byte[] payload) {
    }

    private Tcp parseTcp(final byte[] frame) {
        final ByteBuffer buffer = ByteBuffer.wrap(frame);
        final byte[] destinationMac = new byte[6];
        buffer.get(destinationMac);
        assertArrayEquals(GUEST_MAC, destinationMac, "frame was not addressed to the guest");
        buffer.position(12);
        assertEquals(ETHERTYPE_IPv4, buffer.getShort(), "expected an IPv4 frame");

        final int ipStart = buffer.position();
        final int versionAndHeaderLength = Byte.toUnsignedInt(buffer.get());
        assertEquals(4, versionAndHeaderLength >>> 4);
        final int headerSize = (versionAndHeaderLength & 0xF) * 4;

        buffer.position(ipStart + 9);
        assertEquals(PROTOCOL_TCP, buffer.get(), "expected a TCP packet");
        buffer.position(ipStart + 12);
        assertEquals(0x7F000001, buffer.getInt(), "source should be the address the guest dialled");
        assertEquals(GUEST_IP, buffer.getInt(), "destination should be the guest");

        buffer.position(ipStart + headerSize);
        buffer.getShort(); // Source port.
        buffer.getShort(); // Destination port.

        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(buffer), "stack emitted an unparsable TCP header");
        final byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        return new Tcp(header, payload);
    }

    private static byte[] destinationMacOf(final byte[] frame) {
        return java.util.Arrays.copyOf(frame, 6);
    }

    private static int arpSenderIpOf(final byte[] frame) {
        // Sender protocol address, which in a reply is the address being answered for.
        return ByteBuffer.wrap(frame).getInt(LinkLocalLayer.FRAME_HEADER_SIZE + 14);
    }

    private static int arpTargetIpOf(final byte[] frame) {
        // Target protocol address, at the end of the ARP payload.
        return ByteBuffer.wrap(frame).getInt(LinkLocalLayer.FRAME_HEADER_SIZE + 24);
    }

    private void resolveGateway() throws InterruptedException {
        send(arpRequest(GUEST_IP, GATEWAY_IP));

        final byte[] reply = pumpUntilFrame();
        final ByteBuffer buffer = ByteBuffer.wrap(reply);
        final byte[] destinationMac = new byte[6];
        buffer.get(destinationMac);
        assertArrayEquals(GUEST_MAC, destinationMac);

        gatewayMac = new byte[6];
        buffer.get(gatewayMac);
        assertEquals(ETHERTYPE_ARP, buffer.getShort());

        buffer.position(LinkLocalLayer.FRAME_HEADER_SIZE + 6);
        assertEquals(2, buffer.getShort(), "expected an ARP reply");
        final byte[] senderMac = new byte[6];
        buffer.get(senderMac);
        assertArrayEquals(gatewayMac, senderMac, "the reply should advertise the gateway's own MAC");
        assertEquals(GATEWAY_IP, buffer.getInt(), "the gateway should answer to the address it was asked for");
    }
}
