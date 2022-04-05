package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.Config;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class StreamSessionImpl extends SessionBase implements StreamSession {
    private static final Random random = new Random();

    private final StreamSessionDiscriminator discriminator;

    // Data from session layer implementation
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(Config.streamBufferSize);
    private int receiveWindow = 0;
    private int nextSegmentMark = 0; // for retransmission

    // Data from virtual machine
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(Config.streamBufferSize);

    private int mySequence = random.nextInt();
    private int vmSequence;

    private final TcpHeader header = new TcpHeader();

    private boolean needsAcknowledgment = false;
    private Instant retransmitTime = Instant.now();

    /////////////////////////////////////////////////////////////////////////

    public StreamSessionImpl(
            final int ipAddress,
            final short port,
            final StreamSessionDiscriminator discriminator
    ) {
        super(ipAddress, port);
        this.discriminator = discriminator;
        sendBuffer.limit(0);
    }

    private int computeWindow() {
        return sendBuffer.remaining();
    }

    public boolean newConnection(final ByteBuffer data) {
        final boolean correct = header.read(data);
        if (!correct) {
            return false;
        }
        final boolean isInitiation = header.isConnectionInitiation();
        if (!isInitiation) {
            return false;
        }
        vmSequence = header.sequenceNumber;
        receiveWindow = header.window;
        return true;
    }

    private void acceptConnection(final ByteBuffer data) {
        header.acceptConnection(mySequence++, ++vmSequence, computeWindow());
        header.write(data);
        data.flip();
        setState(States.ESTABLISHED);
    }

    private void denyConnection(final ByteBuffer data) {
        header.denyConnection(mySequence, vmSequence + 1);
        header.write(data);
        data.flip();
    }

    private boolean onPacket(final ByteBuffer data) {
        final boolean correct = header.read(data);
        if (!correct) {
            return false;
        }
        if (header.syn) {
            return false;
        }
        if (header.sequenceNumber != vmSequence) {
            return false;
        }
        if (header.ack) {
            // Segment received
            if (header.acknowledgmentNumber != mySequence) {
                return false;
            }
            receiveWindow = header.window;
            final int newPosition = receiveBuffer.position() - nextSegmentMark;
            receiveBuffer.position(nextSegmentMark);
            receiveBuffer.compact();
            receiveBuffer.position(newPosition);
            receiveBuffer.limit(receiveBuffer.capacity());
            nextSegmentMark = 0;
        } else {
            receiveWindow = header.window;
        }
        if (header.psh) {
            // Data to be sent
            final int length = data.remaining();
            if (length > computeWindow()) {
                // TODO: State changed, but packet rejected
                return false;
            }
            vmSequence += length;
            sendBuffer.compact();
            sendBuffer.limit(sendBuffer.limit() + length);
            sendBuffer.put(data);
            needsAcknowledgment = true;
        }
        if (header.fin) {
            setState(States.FINISH);
            ++vmSequence;
        }
        return true;
    }

    private void pushNextReceivedDataTo(final ByteBuffer data) {
        final int position = data.position();
        data.position(position + TcpHeader.MIN_HEADER_SIZE_NO_PORTS);

        // Copy payload (yes, it is easier to prepare payload first)
        final int recvPos = receiveBuffer.position();
        final int recvLim = receiveBuffer.limit();
        receiveBuffer.limit(nextSegmentMark);
        receiveBuffer.position(0);
        data.put(receiveBuffer);
        receiveBuffer.position(recvPos);
        receiveBuffer.limit(recvLim);
        data.position(position);

        // Update time
        retransmitTime = Instant.now().plus(Config.tcpRetransmissionTimeoutMs, ChronoUnit.MILLIS);
    }

    private boolean preparePacket(final ByteBuffer data) {
        final int length = receiveBuffer.position();
        header.urg = false;
        header.syn = false;
        header.rst = false;
        header.ack = needsAcknowledgment;
        header.sequenceNumber = mySequence - nextSegmentMark;
        header.acknowledgmentNumber = vmSequence;
        header.maxSegmentSize = -1;
        header.urgentPointer = 0;
        header.window = computeWindow();
        header.psh = length != 0;
        if (header.psh) {
            header.fin = false;
            // We have something to receive
            if (nextSegmentMark == 0) {
                // Acknowledged, prepare next segment
                nextSegmentMark = Math.min(Math.min(receiveWindow, length), data.remaining() - TcpHeader.MIN_HEADER_SIZE_NO_PORTS);
                mySequence += nextSegmentMark;
                pushNextReceivedDataTo(data);
            } else {
                // Packet is already sent, is retransmission required?
                if (retransmitTime.compareTo(Instant.now()) > 0) {
                    return false; // no
                } else {
                    pushNextReceivedDataTo(data);
                }
            }
        } else {
            header.fin = getState() == States.FINISH;
            header.window = 0;
        }
        header.write(data);
        return true;
    }

    public boolean onSend(final ByteBuffer data) {
        return switch (getState()) {
            case NEW -> newConnection(data);
            case ESTABLISHED, FINISH -> onPacket(data);
            case REJECT, EXPIRED -> throw new IllegalStateException();
        };
    }

    public boolean onReceive(final ByteBuffer data) {
        switch (getState()) {
            case NEW:
                acceptConnection(data);
                return true;
            case ESTABLISHED:
            case FINISH:
                return preparePacket(data);
            case REJECT:
                denyConnection(data);
                return true;
            case EXPIRED:
                throw new IllegalStateException();
        }
        return false;
    }

    @Override
    public StreamSessionDiscriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public ByteBuffer getReceiveBuffer() {
        return receiveBuffer;
    }

    @Override
    public ByteBuffer getSendBuffer() {
        return sendBuffer;
    }

    @Nullable
    public Instant whenCoolOff() {
        if (nextSegmentMark != 0) {
            return retransmitTime;
        } else {
            return null;
        }
    }

    @Override
    public void connect() {
        if (getState() != States.NEW)
            throw new IllegalStateException();
        setState(States.ESTABLISHED);
    }

    public TcpHeader getHeader() {
        return header;
    }
}
