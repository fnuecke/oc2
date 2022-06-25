package li.cil.oc2.common.inet;

public abstract class DatagramSessionBase extends SessionBase {

    private States state = States.NEW;

    public DatagramSessionBase(final int ipAddress, final short port) {
        super(ipAddress, port);
    }

    @Override
    public void close() {
        switch (state) {
            case NEW -> state = States.REJECT;
            case ESTABLISHED -> state = States.FINISH;
            default -> throw new IllegalStateException();
        }
    }

    @Override
    public States getState() {
        return state;
    }

    @Override
    public void expire() {
        state = States.EXPIRED;
    }

    public void setState(final States state) {
        this.state = state;
    }
}
