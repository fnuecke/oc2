package li.cil.oc2.common;

import li.cil.oc2.common.capabilities.DeviceBusElementCapability;
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
        DeviceBusElementCapability.register();
        Providers.initialize();
        Network.setup();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(IMC::handleIMCMessages);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(CommonSetup::handleServerStoppedEvent);
        ServerScheduler.register();

        addBuiltinDeviceMethodParameterTypeAdapters();
    }

    public static void handleServerAboutToStart(final FMLServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    public static void handleServerStoppedEvent(final FMLServerStoppedEvent event) {
        BlobStorage.synchronize();
        Allocator.resetAndCheckLeaks();
    }

    private static void addBuiltinDeviceMethodParameterTypeAdapters() {
        DeviceMethodParameterTypeAdapters.addTypeAdapter(ItemStack.class, new ItemStackJsonSerializer());
    }
}
