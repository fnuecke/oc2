/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

public final class EchoSession extends AbstractSession {
    private final SessionKey.Echo key;
    private SessionState state = SessionState.NEW;
    private int sequenceNumber;
    private int timeToLive;

    // --------------------------------------------------------------------- //

    public EchoSession(final SessionKey.Echo key, final short port) {
        super(key.destinationIpAddress(), port);
        this.key = key;
    }

    // --------------------------------------------------------------------- //

    @Override
    public SessionKey.Echo getKey() {
        return key;
    }

    @Override
    public SessionState getState() {
        return state;
    }

    public void setState(final SessionState state) {
        this.state = state;
    }

    @Override
    public void close() {
        if (state == SessionState.NEW || state == SessionState.ESTABLISHED) {
            state = SessionState.FINISH;
        }
    }

    @Override
    public void expire() {
        if (state == SessionState.NEW || state == SessionState.ESTABLISHED) {
            state = SessionState.EXPIRED;
        }
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final short sequenceNumber) {
        this.sequenceNumber = Short.toUnsignedInt(sequenceNumber);
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(final byte timeToLive) {
        this.timeToLive = Byte.toUnsignedInt(timeToLive);
    }

    @Override
    public String toString() {
        return "EchoSession(" + key + ")";
    }
}
