package li.cil.oc2.common;

import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.bus.device.rpc.RPCItemStackTagFilters;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.util.ServerScheduler;
import net.fabricmc.api.ModInitializer;

public final class CommonSetup {

    public void onInitialize() {
        Capabilities.initialize();
        FileSystems.initialize();
        Network.initialize();
        RPCItemStackTagFilters.initialize();
        RPCMethodParameterTypeAdapters.initialize();
        ServerScheduler.initialize();
    }
    /*
    @SubscribeEvent
    public static void handleSetupEvent(final FMLCommonSetupEvent event) {

        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerStopped);
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleServerAboutToStart(final FMLServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    private static void handleServerStopped(final FMLServerStoppedEvent event) {
        BlobStorage.synchronize();
        Allocator.resetAndCheckLeaks();
        FileSystems.reset();
    } */
}
