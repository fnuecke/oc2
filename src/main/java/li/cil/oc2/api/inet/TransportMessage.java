package li.cil.oc2.api.inet;

import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.common.inet.InetUtils;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Reusable data object, that contains information about transport layer message that makes sense both for
 * transport and network layer.
 */
public final class TransportMessage {

    private static final byte DEFAULT_TTL = 64;

    ///////////////////////////////////////////////////////////////////////

    private short networkProtocolNumber = -1;
    private long srcIpAddressMost = -1;
    private long srcIpAddressLeast = -1;
    private long dstIpAddressMost = -1;
    private long dstIpAddressLeast = -1;
    private byte ttl = -1;
    private ByteBuffer data = null;

    ///////////////////////////////////////////////////////////////////////

    /**
     * Network layer should provide byte buffer for transport layer via this method.
     *
     * @param data byte buffer for transport layer data
     */
    public void initializeBuffer(final ByteBuffer data) {
        this.data = data;
    }

    /**
     * Updates network layer parameters for current transport message.
     *
     * @param networkProtocolNumber chosen network protocol number (use {@link NetworkLayer#PROTOCOL_IPv4} or
     *                              {@link NetworkLayer#PROTOCOL_IPv6} values)
     * @param srcIpAddressMost      part of a source IP address
     * @param srcIpAddressLeast     part of a source IP address
     * @param dstIpAddressMost      part of a destination IP address
     * @param dstIpAddressLeast     part of a destination IP address
     * @param ttl                   time to live value (for tracert functionality)
     */
    public void update(
            final short networkProtocolNumber,
            final long srcIpAddressMost,
            final long srcIpAddressLeast,
            final long dstIpAddressMost,
            final long dstIpAddressLeast,
            final byte ttl
    ) {
        this.networkProtocolNumber = networkProtocolNumber;
        this.srcIpAddressMost = srcIpAddressMost;
        this.srcIpAddressLeast = srcIpAddressLeast;
        this.dstIpAddressMost = dstIpAddressMost;
        this.dstIpAddressLeast = dstIpAddressLeast;
        this.ttl = ttl;
    }

    /**
     * Updates network layer parameters for current transport message assuming IPv4 network protocol.
     *
     * @param srcIpAddress source IP address
     * @param dstIpAddress destination IP address
     * @param ttl          time to live value (for tracert functionality)
     */
    public void updateIpv4(
            final int srcIpAddress,
            final int dstIpAddress,
            final byte ttl
    ) {
        update(NetworkLayer.PROTOCOL_IPv4, 0, srcIpAddress, 0, dstIpAddress, ttl);
    }

    /**
     * Updates network layer parameters for current transport message and sets the default TTL value assuming IPv4
     * network protocol.
     *
     * @param srcIpAddress source IP address
     * @param dstIpAddress destination IP address
     */
    public void updateIpv4(
            final int srcIpAddress,
            final int dstIpAddress
    ) {
        updateIpv4(srcIpAddress, dstIpAddress, DEFAULT_TTL);
    }

    /**
     * Gets stored TTL value.
     *
     * @return TTL value
     */
    public byte getTtl() {
        return ttl;
    }

    /**
     * Gets stored source IPv4 address.
     *
     * @return IPv4 source address
     */
    public int getSrcIpv4Address() {
        return (int) srcIpAddressLeast;
    }

    /**
     * Gets stored destination IPv4 address.
     *
     * @return IPv4 destination address
     */
    public int getDstIpv4Address() {
        return (int) dstIpAddressLeast;
    }

    /**
     * Gets stored source IP address as {@link InetAddress} object.
     *
     * @return IPv4 source address
     */
    public InetAddress getSrcAddress() {
        return switch (networkProtocolNumber) {
            case NetworkLayer.PROTOCOL_IPv4 -> InetUtils.toJavaInetAddress(getSrcIpv4Address());
            case NetworkLayer.PROTOCOL_IPv6 -> InetUtils.toJavaInetAddress(srcIpAddressMost, srcIpAddressLeast);
            default -> throw new IllegalStateException();
        };
    }

    /**
     * Gets stored destination IP address as {@link InetAddress} object.
     *
     * @return IPv4 destination address
     */
    public InetAddress getDstAddress() {
        return switch (networkProtocolNumber) {
            case NetworkLayer.PROTOCOL_IPv4 -> InetUtils.toJavaInetAddress(getDstIpv4Address());
            case NetworkLayer.PROTOCOL_IPv6 -> InetUtils.toJavaInetAddress(dstIpAddressMost, dstIpAddressLeast);
            default -> throw new IllegalStateException();
        };
    }

    /**
     * Gets transport layer data buffer
     *
     * @return transport layer data buffer
     */
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalStateException();
        }
        return data;
    }

    /**
     * Gets network protocol number
     *
     * @return network protocol number
     */
    public short getNetworkProtocolNumber() {
        return networkProtocolNumber;
    }

    /**
     * Checks if an IPv4 transport message stored in this object.
     *
     * @return true only if it is an IPv4 message
     */
    public boolean isIpv4() {
        return networkProtocolNumber == NetworkLayer.PROTOCOL_IPv4;
    }

    /**
     * Checks if an IPv6 transport message stored in this object.
     *
     * @return true only if it is an IPv6 message
     */
    public boolean isIpv6() {
        return networkProtocolNumber == NetworkLayer.PROTOCOL_IPv6;
    }
}
