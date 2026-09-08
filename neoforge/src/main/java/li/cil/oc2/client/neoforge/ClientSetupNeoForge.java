/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.*;
import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.model.neoforge.BusCableModelLoader;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.client.renderer.blockentity.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.DiskDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.FlashDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.NetworkConnectorRenderer;
import li.cil.oc2.client.renderer.blockentity.neoforge.ChargerRendererNeoForge;
import li.cil.oc2.client.renderer.blockentity.neoforge.ProjectorRendererNeoForge;
import li.cil.oc2.client.renderer.color.BusCableBlockColor;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.RobotWithoutLevelRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ClientSetupNeoForge {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        ComputerRenderer.initialize();
        ProjectorDepthRenderer.initialize();

        event.enqueueWork(() -> {
            CustomItemModelProperties.initialize();
            CustomItemColors.initialize();

            ColorHandlerRegistry.registerBlockColors(new BusCableBlockColor(), Blocks.BUS_CABLE);
        });
    }

    @SubscribeEvent
    public static void handleRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntities.COMPUTER.get(), ComputerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.FLASH_DRIVE.get(), FlashDriveRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.CHARGER.get(), ChargerRendererNeoForge::new);
        event.registerBlockEntityRenderer(BlockEntities.PROJECTOR.get(), ProjectorRendererNeoForge::new);
        event.registerBlockEntityRenderer(BlockEntities.NETWORK_CONNECTOR.get(), NetworkConnectorRenderer::new);

        event.registerEntityRenderer(Entities.ROBOT.get(), RobotRenderer::new);
    }

    @SubscribeEvent
    public static void handleRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(Containers.COMPUTER.get(), ComputerContainerScreen::new);
        event.register(Containers.COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
        event.register(Containers.ROBOT.get(), RobotContainerScreen::new);
        event.register(Containers.ROBOT_TERMINAL.get(), RobotTerminalScreen::new);
        event.register(Containers.NETWORK_TUNNEL.get(), NetworkTunnelScreen::new);
    }

    @SubscribeEvent
    public static void handleRegisterGeometryLoaders(final ModelEvent.RegisterGeometryLoaders event) {
        event.register(Blocks.BUS_CABLE.getId(), new BusCableModelLoader());
    }

    @SubscribeEvent
    public static void handleRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);
    }

    @SubscribeEvent
    public static void handleRegisterClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Nullable
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    final Minecraft minecraft = Minecraft.getInstance();
                    renderer = new RobotWithoutLevelRenderer(
                        minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        }, Items.ROBOT.get());
    }

    private ClientSetupNeoForge() {
    }
}
