package li.cil.oc2.client;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.model.BusCableModelLoader;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.client.renderer.tileentity.ComputerTileEntityRenderer;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientSetup {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        NetworkCableRenderer.initialize();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(DeviceTypes::handleTextureStitchEvent);
        ScreenManager.registerFactory(Containers.COMPUTER_CONTAINER.get(), ComputerContainerScreen::new);

        ClientRegistry.bindTileEntityRenderer(TileEntities.COMPUTER_TILE_ENTITY.get(), ComputerTileEntityRenderer::new);
    }

    @SubscribeEvent
    public static void handleModelRegistryEvent(final ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(new ResourceLocation(API.MOD_ID, Constants.BUS_CABLE_BLOCK_NAME), new BusCableModelLoader());
    }
}
