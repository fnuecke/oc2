package li.cil.oc2.common;

import li.cil.oc2.common.blobs.BlobStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.sandbox.Allocator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

public final class CommonSetup {
    public static void run(final FMLCommonSetupEvent event) {
        Network.setup();

        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerStoppedEvent);
    }

    public static void handleServerAboutToStart(final FMLServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    public static void handleServerStoppedEvent(final FMLServerStoppedEvent event) {
        BlobStorage.synchronize();
        Allocator.resetAndCheckLeaks();
    }
}
