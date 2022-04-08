package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.EchoSession;

public final class EchoSessionImpl extends SessionBase implements EchoSession {
    private final EchoSessionDiscriminator discriminator;
    private byte ttl;
    private short sequenceNumber;

    public EchoSessionImpl(final int ipAddress, final short port, final EchoSessionDiscriminator discriminator) {
        super(ipAddress, port);
        this.discriminator = discriminator;
    }

    @Override
    public int getTtl() {
        return Byte.toUnsignedInt(ttl);
    }

    public void setTtl(final byte ttl) {
        this.ttl = ttl;
    }

    @Override
    public int getSequenceNumber() {
        return Short.toUnsignedInt(sequenceNumber);
    }

    public void setSequenceNumber(final short sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public EchoSessionDiscriminator getDiscriminator() {
        return discriminator;
    }
}
