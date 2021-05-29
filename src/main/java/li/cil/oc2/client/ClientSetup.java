package li.cil.oc2.client;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.client.gui.ComputerInventoryScreen;
import li.cil.oc2.client.gui.ComputerTerminalScreen;
import li.cil.oc2.client.gui.RobotContainerScreen;
import li.cil.oc2.client.gui.RobotTerminalScreen;
import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.model.BusCableModelLoader;
import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.client.renderer.entity.RobotEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.ChargerTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.ComputerTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.DiskDriveTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.NetworkConnectorTileEntityRenderer;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        NetworkCableRenderer.initialize();
        BusInterfaceNameRenderer.initialize();
        CustomItemModelProperties.initialize();
        CustomItemColors.initialize();

        ScreenManager.register(Containers.COMPUTER_CONTAINER.get(), ComputerInventoryScreen::new);
        ScreenManager.register(Containers.COMPUTER_TERMINAL_CONTAINER.get(), ComputerTerminalScreen::new);
        ScreenManager.register(Containers.ROBOT_CONTAINER.get(), RobotContainerScreen::new);
        ScreenManager.register(Containers.ROBOT_TERMINAL_CONTAINER.get(), RobotTerminalScreen::new);

        ClientRegistry.bindTileEntityRenderer(TileEntities.COMPUTER_TILE_ENTITY.get(), ComputerTileEntityRenderer::new);
        ClientRegistry.bindTileEntityRenderer(TileEntities.NETWORK_CONNECTOR_TILE_ENTITY.get(), NetworkConnectorTileEntityRenderer::new);
        ClientRegistry.bindTileEntityRenderer(TileEntities.DISK_DRIVE_TILE_ENTITY.get(), DiskDriveTileEntityRenderer::new);
        ClientRegistry.bindTileEntityRenderer(TileEntities.CHARGER_TILE_ENTITY.get(), ChargerTileEntityRenderer::new);

        RenderingRegistry.registerEntityRenderingHandler(Entities.ROBOT.get(), RobotEntityRenderer::new);
    }

    @SubscribeEvent
    public static void handleModelRegistryEvent(final ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(Blocks.BUS_CABLE.getId(), new BusCableModelLoader());
    }

    @SubscribeEvent
    public static void handleTextureStitchEvent(final TextureStitchEvent.Pre event) {
        if (event.getMap().location() != PlayerContainer.BLOCK_ATLAS) {
            return;
        }

        for (final DeviceType deviceType : DeviceTypes.DEVICE_TYPE_REGISTRY.get().getValues()) {
            event.addSprite(deviceType.getBackgroundIcon());
        }

        event.addSprite(ComputerTileEntityRenderer.OVERLAY_POWER_LOCATION);
        event.addSprite(ComputerTileEntityRenderer.OVERLAY_STATUS_LOCATION);
        event.addSprite(ComputerTileEntityRenderer.OVERLAY_TERMINAL_LOCATION);

        event.addSprite(ChargerTileEntityRenderer.EFFECT_LOCATION);
    }
}
