package li.cil.oc2.api.inet;

public interface EchoSession extends Session {
    int getSequenceNumber();

    int getTtl();
}
