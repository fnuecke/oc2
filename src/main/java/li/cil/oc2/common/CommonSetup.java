package li.cil.oc2.common;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.device.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.device.provider.Providers;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.vm.Allocator;
import li.cil.oc2.serialization.BlobStorage;
import li.cil.oc2.serialization.serializers.ItemStackJsonSerializer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class CommonSetup {
    public static void run(final FMLCommonSetupEvent event) {
        Capabilities.initialize();

        Providers.initialize();
        Network.setup();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(IMC::handleIMCMessages);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerStoppedEvent);
        ServerScheduler.register();

        addBuiltinRPCMethodParameterTypeAdapters();
    }

    public static void handleServerAboutToStart(final FMLServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    public static void handleServerStoppedEvent(final FMLServerStoppedEvent event) {
        BlobStorage.synchronize();
        Allocator.resetAndCheckLeaks();
    }

    private static void addBuiltinRPCMethodParameterTypeAdapters() {
        RPCMethodParameterTypeAdapters.addTypeAdapter(ItemStack.class, new ItemStackJsonSerializer());
    }
}
