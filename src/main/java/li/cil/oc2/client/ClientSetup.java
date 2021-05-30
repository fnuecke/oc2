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
import li.cil.oc2.client.renderer.tileentity.ChargerBlockEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.ComputerBlockEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.DiskDriveBlockEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.NetworkConnectorBlockEntityRenderer;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screens.MenuScreens;
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

        MenuScreens.register(Containers.COMPUTER_CONTAINER.get(), ComputerInventoryScreen::new);
        MenuScreens.register(Containers.COMPUTER_TERMINAL_CONTAINER.get(), ComputerTerminalScreen::new);
        MenuScreens.register(Containers.ROBOT_CONTAINER.get(), RobotContainerScreen::new);
        MenuScreens.register(Containers.ROBOT_TERMINAL_CONTAINER.get(), RobotTerminalScreen::new);

        ClientRegistry.bindBlockEntityRenderer(TileEntities.COMPUTER_TILE_ENTITY.get(), ComputerBlockEntityRenderer::new);
        ClientRegistry.bindBlockEntityRenderer(TileEntities.NETWORK_CONNECTOR_TILE_ENTITY.get(), NetworkConnectorBlockEntityRenderer::new);
        ClientRegistry.bindBlockEntityRenderer(TileEntities.DISK_DRIVE_TILE_ENTITY.get(), DiskDriveBlockEntityRenderer::new);
        ClientRegistry.bindBlockEntityRenderer(TileEntities.CHARGER_TILE_ENTITY.get(), ChargerBlockEntityRenderer::new);

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

        event.addSprite(ComputerBlockEntityRenderer.OVERLAY_POWER_LOCATION);
        event.addSprite(ComputerBlockEntityRenderer.OVERLAY_STATUS_LOCATION);
        event.addSprite(ComputerBlockEntityRenderer.OVERLAY_TERMINAL_LOCATION);

        event.addSprite(ChargerBlockEntityRenderer.EFFECT_LOCATION);
    }
}
