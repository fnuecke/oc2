package li.cil.oc2.common;

import li.cil.oc2.common.network.Network;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CommonSetup {
    public static void run(final FMLCommonSetupEvent event) {
        Network.setup();
    }
}
