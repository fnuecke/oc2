package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.DatagramSession;

public final class DatagramSessionImpl extends SessionBase implements DatagramSession {
    private final DatagramSessionDiscriminator discriminator;

    public DatagramSessionImpl(final int ipAddress, final short port, final DatagramSessionDiscriminator discriminator) {
        super(ipAddress, port);
        this.discriminator = discriminator;
    }

    @Override
    public DatagramSessionDiscriminator getDiscriminator() {
        return discriminator;
    }
}
