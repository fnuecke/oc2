package li.cil.oc2.common.inet;

import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

public interface InternetAdapter {
    @Nullable
    byte[] receiveEthernetFrame();

    void sendEthernetFrame(byte[] frame);
}
