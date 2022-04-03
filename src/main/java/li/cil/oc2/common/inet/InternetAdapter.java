package li.cil.oc2.common.inet;

import javax.annotation.Nullable;

public interface InternetAdapter {
    @Nullable
    byte[] receiveEthernetFrame();

    void sendEthernetFrame(byte[] frame);
}
