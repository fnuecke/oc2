package li.cil.oc2.api.inet.session;

public interface EchoSession extends Session {
    int getSequenceNumber();

    int getTtl();
}
