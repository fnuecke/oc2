package li.cil.oc2.common;

import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.IMC;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.serializers.DirectionJsonSerializer;
import li.cil.oc2.common.serialization.serializers.ItemStackJsonSerializer;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.vm.Allocator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class CommonSetup {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLCommonSetupEvent event) {
        Capabilities.initialize();

        Network.setup();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(IMC::handleIMCMessages);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerStopped);
        MinecraftForge.EVENT_BUS.addListener(FileSystems::handleAddReloadListenerEvent);
        ServerScheduler.register();

        addBuiltinRPCMethodParameterTypeAdapters();
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleServerAboutToStart(final FMLServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    private static void handleServerStopped(final FMLServerStoppedEvent event) {
        BlobStorage.synchronize();
        Allocator.resetAndCheckLeaks();
        FileSystems.reset();
    }

    private static void addBuiltinRPCMethodParameterTypeAdapters() {
        RPCMethodParameterTypeAdapters.addTypeAdapter(ItemStack.class, new ItemStackJsonSerializer());
        RPCMethodParameterTypeAdapters.addTypeAdapter(Direction.class, new DirectionJsonSerializer());
    }
}
