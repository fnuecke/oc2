package li.cil.oc2.client;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.gui.ComputerTerminalScreen;
import li.cil.oc2.client.gui.RobotContainerScreen;
import li.cil.oc2.client.gui.RobotTerminalScreen;
import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.model.BusCableModelLoader;
import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.client.renderer.color.BusCableBlockColor;
import li.cil.oc2.client.renderer.entity.RobotEntityRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.client.renderer.tileentity.ChargerTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.ComputerTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.DiskDriveTileEntityRenderer;
import li.cil.oc2.client.renderer.tileentity.NetworkConnectorTileEntityRenderer;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Objects;

public final class ClientSetup {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        NetworkCableRenderer.initialize();
        BusInterfaceNameRenderer.initialize();

        BlockEntityRenderers.register(TileEntities.COMPUTER_TILE_ENTITY.get(), ComputerTileEntityRenderer::new);
        BlockEntityRenderers.register(TileEntities.NETWORK_CONNECTOR_TILE_ENTITY.get(), NetworkConnectorTileEntityRenderer::new);
        BlockEntityRenderers.register(TileEntities.DISK_DRIVE_TILE_ENTITY.get(), DiskDriveTileEntityRenderer::new);
        BlockEntityRenderers.register(TileEntities.CHARGER_TILE_ENTITY.get(), ChargerTileEntityRenderer::new);

        event.enqueueWork(() -> {
            CustomItemModelProperties.initialize();
            CustomItemColors.initialize();

            MenuScreens.register(Containers.COMPUTER.get(), ComputerContainerScreen::new);
            MenuScreens.register(Containers.COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
            MenuScreens.register(Containers.ROBOT.get(), RobotContainerScreen::new);
            MenuScreens.register(Containers.ROBOT_TERMINAL.get(), RobotTerminalScreen::new);

            ItemBlockRenderTypes.setRenderLayer(Blocks.BUS_CABLE.get(), (RenderType) -> true);
            Minecraft.getInstance().getBlockColors().register(new BusCableBlockColor(), Blocks.BUS_CABLE.get());
        });
    }

    @SubscribeEvent
    public static void handleModelRegistryEvent(final ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(Blocks.BUS_CABLE.getId(), new BusCableModelLoader());
    }

    @SubscribeEvent
    public static void handleTextureStitchEvent(final TextureStitchEvent.Pre event) {
        if (!Objects.equals(event.getAtlas().location(), InventoryMenu.BLOCK_ATLAS)) {
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

    @SubscribeEvent
    public static void handleEntityRendererRegisterEvent(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Entities.ROBOT.get(), RobotEntityRenderer::new);
    }

    @SubscribeEvent
    public static void handleRegisterLayerDefinitionsEvent(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);
    }
}
