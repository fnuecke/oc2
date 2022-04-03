package li.cil.oc2.common.inet;

import java.util.Objects;

public abstract class SocketSessionDiscriminator<S extends SessionBase> implements SessionDiscriminator<S> {
    private final int srcIpAddress;
    private final short srcPort;
    private final int dstIpAddress;
    private final short dstPort;

    public SocketSessionDiscriminator(
            final int srcIpAddress,
            final short srcPort,
            final int dstIpAddress,
            final short dstPort
    ) {
        this.srcIpAddress = srcIpAddress;
        this.srcPort = srcPort;
        this.dstIpAddress = dstIpAddress;
        this.dstPort = dstPort;
    }

    public int getSrcIpAddress() {
        return srcIpAddress;
    }

    public short getSrcPort() {
        return srcPort;
    }

    public int getDstIpAddress() {
        return dstIpAddress;
    }

    public short getDstPort() {
        return dstPort;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocketSessionDiscriminator<?> that = (SocketSessionDiscriminator<?>) o;
        return srcIpAddress == that.srcIpAddress
               && srcPort == that.srcPort
               && dstIpAddress == that.dstIpAddress
               && dstPort == that.dstPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), srcIpAddress, srcPort, dstIpAddress, dstPort);
    }

    abstract String protocolName();

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(protocolName());
        builder.append('(');
        InetUtils.socketAddressToString(builder, srcIpAddress, srcPort);
        builder.append("<->");
        InetUtils.socketAddressToString(builder, dstIpAddress, dstPort);
        builder.append(')');
        return builder.toString();
    }
}
