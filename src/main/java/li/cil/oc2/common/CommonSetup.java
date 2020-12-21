package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.init.*;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.serializers.ItemStackJsonSerializer;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.vm.Allocator;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

public final class CommonSetup implements ModInitializer {
    @Override
    public void onInitialize() {
        // Do class lookup in a separate thread to avoid blocking for too long.
        // Specifically, this is to run detection of annotated types via the Reflections
        // library in the serialization library and the device tree registry.
        new Thread(() -> {
            Ceres.initialize();
            DeviceTreeRegistry.initialize();
        }).start();

        Items.initialize();
        Blocks.initialize();
        TileEntities.initialize();
        Containers.initialize();
        Providers.initialize();

        Network.setup();

        ServerLifecycleEvents.SERVER_STARTING.register(CommonSetup::handleServerAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(CommonSetup::handleServerStopped);
        ServerScheduler.register();

        addBuiltinRPCMethodParameterTypeAdapters();
    }

    public static void handleServerAboutToStart(final MinecraftServer server) {
        BlobStorage.setServer(server);
    }

    public static void handleServerStopped(final MinecraftServer server) {
        BlobStorage.synchronize();
        Allocator.resetAndCheckLeaks();
    }

    ///////////////////////////////////////////////////////////////////

    private static void addBuiltinRPCMethodParameterTypeAdapters() {
        RPCMethodParameterTypeAdapters.addTypeAdapter(ItemStack.class, new ItemStackJsonSerializer());
    }
}
