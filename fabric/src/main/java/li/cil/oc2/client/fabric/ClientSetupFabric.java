/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import li.cil.oc2.api.API;
import li.cil.oc2.client.audio.SoundCardAudio;
import li.cil.oc2.client.gui.*;
import li.cil.oc2.client.model.fabric.BusCableModelLoader;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.client.renderer.TerminalTextures;
import li.cil.oc2.client.renderer.blockentity.ChargerRenderer;
import li.cil.oc2.client.renderer.blockentity.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.DiskDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.FlashDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.ProjectorRenderer;
import li.cil.oc2.client.renderer.blockentity.TerminalRenderer;
import li.cil.oc2.client.renderer.color.BusCableBlockColor;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.RobotSpecialRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;

public final class ClientSetupFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TerminalTextures.initialize();
        ProjectorDepthRenderer.initialize();
        SoundCardAudio.initialize();
        DeviceHints.initialize();
        ModRenderType.initialize();
        ClientLevelEventsFabric.initialize();
        ClientRenderEventsFabric.initialize();

        BusCableModelLoader.initialize();
        ColorHandlerRegistry.registerBlockColors(new BusCableBlockColor(), Blocks.BUS_CABLE);
        RenderTypeRegistry.register(ChunkSectionLayer.CUTOUT, Blocks.TRANSPOSER.get());

        BlockEntityRendererRegistry.register(BlockEntities.COMPUTER.get(), ComputerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.FLASH_DRIVE.get(), FlashDriveRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.CHARGER.get(), ChargerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PROJECTOR.get(), ProjectorRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.TERMINAL.get(), TerminalRenderer::new);

        EntityRendererRegistry.register(Entities.ROBOT, RobotRenderer::new);
        EntityModelLayerRegistry.register(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);

        MenuScreens.register(Containers.COMPUTER.get(), ComputerContainerScreen::new);
        MenuScreens.register(Containers.COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
        MenuScreens.register(Containers.ROBOT.get(), RobotContainerScreen::new);
        MenuScreens.register(Containers.ROBOT_TERMINAL.get(), RobotTerminalScreen::new);
        MenuScreens.register(Containers.NETWORK_TUNNEL.get(), NetworkTunnelScreen::new);

        SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(API.MOD_ID, "robot"), RobotSpecialRenderer.Unbaked.MAP_CODEC);
        SpecialGuiElementRegistry.register(context -> new SideConfigurationBlockRenderer(context.vertexConsumers()));
    }
}
