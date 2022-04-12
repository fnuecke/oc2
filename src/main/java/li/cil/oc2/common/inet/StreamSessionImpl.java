package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Random;

public class StreamSessionImpl extends SessionBase implements StreamSession {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Random random = new Random();

    private final StreamSessionDiscriminator discriminator;

    // Data from session layer implementation
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(Config.streamBufferSize);
    private int vmWindow = 0;
    private int nextSegmentMark = 0; // for retransmission

    // Data from virtual machine
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(Config.streamBufferSize);

    private int mySequence = random.nextInt();
    private int vmSequence;

    private final TcpHeader header = new TcpHeader();

    private TcpStates state = TcpStates.CONNECT;

    private boolean needsAcknowledgment = false;

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

    public SessionActions receive(final ByteBuffer segment) {
        return state.receive(this, segment);
    }

    public SessionActions send(final ByteBuffer segment) {
        return state.send(this, segment);
    }

    boolean isNeedsAcknowledgment() {
        return needsAcknowledgment;
    }

    @Override
    public ByteBuffer getReceiveBuffer() {
        switch (state) {
            case EXPIRED, FINISH, REJECT -> throw new IllegalStateException();
        }
        return receiveBuffer;
    }

    @Override
    public ByteBuffer getSendBuffer() {
        switch (state) {
            case EXPIRED, REJECT -> throw new IllegalStateException();
        }
        return sendBuffer;
    }

    @Override
    public StreamSessionDiscriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public void expire() {
        state = TcpStates.EXPIRED;
    }

    @Override
    public void connect() {
        if (state != TcpStates.CONNECT) {
            throw new IllegalStateException();
        }
        state = TcpStates.ACCEPT;
    }

    @Override
    public States getState() {
        return state.toSessionState();
    }

    @Override
    public void close() {
        state = switch (state) {
            case ESTABLISHED -> TcpStates.FINISH;
            case CONNECT -> TcpStates.REJECT;
            default -> throw new IllegalStateException();
        };
    }

    public TcpHeader getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "StreamSession(" + discriminator + ")";
    }

    private int computeWindow() {
        return sendBuffer.capacity() - sendBuffer.limit();
    }

    private enum TcpStates {
        CONNECT {
            @Override
            SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
                LOGGER.warn("Incorrect session layer implementation. Stream session is not updated.");
                return SessionActions.IGNORE;
            }

            @Override
            SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
                final TcpHeader header = session.header;
                if (!header.read(segment)) {
                    return SessionActions.DROP;
                }
                if (!header.isConnectionInitiation()) {
                    // weird packet; drop whole session
                    return SessionActions.DROP;
                }
                // initialize stream state
                session.vmSequence = header.sequenceNumber;
                session.vmWindow = header.window;
                return SessionActions.FORWARD;
            }

            @Override
            States toSessionState() {
                return States.NEW;
            }
        },
        ACCEPT {
            @Override
            SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
                final TcpHeader header = session.header;
                header.acceptConnection(session.mySequence, session.vmSequence + 1, session.computeWindow());
                header.write(segment);
                segment.flip();
                return SessionActions.FORWARD;
            }

            @Override
            SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
                final TcpHeader header = session.header;
                if (!header.read(segment)) {
                    // strange incorrect packet; let's ignore it
                    return SessionActions.IGNORE;
                }
                if (!header.isAcceptanceOrRejectionAcknowledged()) {
                    return SessionActions.IGNORE;
                }
                session.mySequence += 1;
                session.vmSequence += 1;
                session.state = TcpStates.ESTABLISHED;
                session.vmWindow = header.window;
                // session layer already knows about this session; do not bother it
                return SessionActions.IGNORE;
            }

            @Override
            States toSessionState() {
                return States.ESTABLISHED;
            }
        },
        REJECT {
            @Override
            SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
                final TcpHeader header = session.header;
                header.rejectConnection(session.mySequence, session.vmSequence + 1);
                header.write(segment);
                segment.flip();
                return SessionActions.FORWARD;
            }

            @Override
            SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
                // rejection sent and session should be closed now
                throw new IllegalStateException();
            }

            @Override
            States toSessionState() {
                return States.REJECT;
            }
        },
        ESTABLISHED {
            @Override
            SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
                final TcpHeader header = session.header;
                final ByteBuffer receiveBuffer = session.receiveBuffer;
                if (session.nextSegmentMark == 0) {
                    session.nextSegmentMark = Math.min(Math.min(session.vmWindow, receiveBuffer.position()), segment.remaining() - TcpHeader.MIN_HEADER_SIZE_NO_PORTS);
                    LOGGER.info("Next segment mark: {}", session.nextSegmentMark);
                }
                header.urg = false;
                header.syn = false;
                header.rst = false;
                header.ack = true; //session.needsAcknowledgment;
                header.sequenceNumber = session.mySequence; //- session.nextSegmentMark;
                header.acknowledgmentNumber = /*header.ack ?*/ session.vmSequence /*: 0*/;
                header.maxSegmentSize = -1;
                header.urgentPointer = 0;
                header.psh = session.nextSegmentMark != 0;
                header.window = session.computeWindow();
                if (!header.ack && !header.psh && session.state != TcpStates.FINISH) {
                    // Nothing to send
                    LOGGER.info("Established session nothing to send");
                    return SessionActions.IGNORE;
                }
                if (header.psh) {
                    header.fin = false;
                    header.write(segment);
                    // We have something to receive

                    // Copy payload (yes, it is easier to prepare payload first)
                    final int recvPos = receiveBuffer.position();
                    final int recvLim = receiveBuffer.limit();
                    receiveBuffer.limit(session.nextSegmentMark);
                    receiveBuffer.position(0);
                    segment.put(receiveBuffer);
                    receiveBuffer.limit(recvLim);
                    receiveBuffer.position(recvPos);
                } else {
                    header.fin = session.state == TcpStates.FINISH;
                    header.write(segment);
                }
                segment.flip();
                return SessionActions.FORWARD;
            }

            @Override
            SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
                final TcpHeader header = session.header;
                final boolean correct = header.read(segment);
                if (!correct) {
                    LOGGER.info("Got invalid TCP header");
                    return SessionActions.IGNORE;
                }
                if (header.syn) {
                    LOGGER.info("Got syn on established connection");
                    return SessionActions.IGNORE;
                }
                if (header.sequenceNumber != session.vmSequence) {
                    LOGGER.info("VM sent invalid sequence number (expected {}, got {})", session.vmSequence, header.sequenceNumber);
                    return SessionActions.IGNORE;
                }
                final int length = segment.remaining();
                if (header.psh && length > session.computeWindow()) {
                    LOGGER.info("Received length > window size");
                    return SessionActions.IGNORE;
                }
                if (header.ack) {
                    // Segment received
                    if (header.acknowledgmentNumber != session.mySequence && header.acknowledgmentNumber != (session.mySequence + session.nextSegmentMark)) {
                        LOGGER.info("VM acked wrong number (expected {}, got {})", session.mySequence, header.acknowledgmentNumber);
                        return SessionActions.IGNORE;
                    }
                    if (header.acknowledgmentNumber == (session.mySequence + session.nextSegmentMark)) {
                        final ByteBuffer receiveBuffer = session.receiveBuffer;
                        // Remove acknowledged data from buffer
                        final int newPosition = receiveBuffer.position() - session.nextSegmentMark;
                        receiveBuffer.position(session.nextSegmentMark);
                        receiveBuffer.compact();
                        receiveBuffer.position(newPosition);
                        receiveBuffer.limit(receiveBuffer.capacity());
                        session.mySequence += session.nextSegmentMark;
                        session.nextSegmentMark = 0;
                    }
                }
                session.vmWindow = header.window;
                if (header.psh) {
                    // Data to be sent
                    session.vmSequence += length;
                    final ByteBuffer sendBuffer = session.sendBuffer;
                    sendBuffer.compact();
                    sendBuffer.put(segment);
                    sendBuffer.flip();
                    session.needsAcknowledgment = true;
                }
                if (header.fin) {
                    ++session.vmSequence;
                    session.state = FINISH;
                }
                return SessionActions.FORWARD;
            }

            @Override
            States toSessionState() {
                return States.ESTABLISHED;
            }
        },
        FINISH {
            @Override
            SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
                return SessionActions.DROP;
            }

            @Override
            SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
                return SessionActions.DROP;
            }

            @Override
            States toSessionState() {
                return States.FINISH;
            }
        },
        EXPIRED {
            @Override
            SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
                return SessionActions.DROP;
            }

            @Override
            SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
                return SessionActions.DROP;
            }

            @Override
            States toSessionState() {
                return States.EXPIRED;
            }
        };

        abstract SessionActions receive(StreamSessionImpl session, ByteBuffer segment);

        abstract SessionActions send(StreamSessionImpl session, ByteBuffer segment);

        abstract States toSessionState();
    }
}
