package li.cil.oc2.client;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.model.BusCableModelLoader;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.client.renderer.tileentity.ComputerTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.NetworkConnectorTileEntityRenderer;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        NetworkCableRenderer.initialize();
        CustomItemModelProperties.initialize();

        ScreenManager.registerFactory(Containers.COMPUTER_CONTAINER.get(), ComputerContainerScreen::new);

        ClientRegistry.bindTileEntityRenderer(TileEntities.COMPUTER_TILE_ENTITY.get(), ComputerTileEntityRenderer::new);
        ClientRegistry.bindTileEntityRenderer(TileEntities.NETWORK_CONNECTOR_TILE_ENTITY.get(), NetworkConnectorTileEntityRenderer::new);
    }

    @SubscribeEvent
    public static void handleModelRegistryEvent(final ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(new ResourceLocation(API.MOD_ID, Constants.BUS_CABLE_BLOCK_NAME), new BusCableModelLoader());
    }

    @SubscribeEvent
    public static void handleTextureStitchEvent(final TextureStitchEvent.Pre event) {
        if (event.getMap().getTextureLocation() != PlayerContainer.LOCATION_BLOCKS_TEXTURE) {
            return;
        }

        for (final DeviceType deviceType : DeviceTypes.DEVICE_TYPE_REGISTRY.get().getValues()) {
            event.addSprite(deviceType.getBackgroundIcon());
        }

        event.addSprite(ComputerTileEntityRenderer.OVERLAY_POWER_LOCATION);
        event.addSprite(ComputerTileEntityRenderer.OVERLAY_STATUS_LOCATION);
        event.addSprite(ComputerTileEntityRenderer.OVERLAY_TERMINAL_LOCATION);
    }
}
