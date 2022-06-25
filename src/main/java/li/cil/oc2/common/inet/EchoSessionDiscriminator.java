package li.cil.oc2.common.inet;

import java.util.Objects;

final class EchoSessionDiscriminator implements SessionDiscriminator<EchoSessionImpl> {
    private final int srcIpAddress;
    private final int dstIpAddress;
    private final short identity;

    public EchoSessionDiscriminator(final int srcIpAddress, final int dstIpAddress, final short identity) {
        this.srcIpAddress = srcIpAddress;
        this.dstIpAddress = dstIpAddress;
        this.identity = identity;
    }

    public int getSrcIpAddress() {
        return srcIpAddress;
    }

    public int getDstIpAddress() {
        return dstIpAddress;
    }

    public short getIdentity() {
        return identity;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EchoSessionDiscriminator that = (EchoSessionDiscriminator) o;
        return srcIpAddress == that.srcIpAddress && dstIpAddress == that.dstIpAddress && identity == that.identity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIpAddress, dstIpAddress, identity);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Echo(");
        InetUtils.ipv4AddressToString(builder, srcIpAddress);
        builder.append("<-[");
        builder.append(Short.toUnsignedInt(identity));
        builder.append("]->");
        InetUtils.ipv4AddressToString(builder, dstIpAddress);
        builder.append(')');
        return builder.toString();
    }
}
