/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import li.cil.oc2.client.gui.*;
import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.model.fabric.BusCableModelLoader;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.client.renderer.blockentity.ChargerRenderer;
import li.cil.oc2.client.renderer.blockentity.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.DiskDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.FlashDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.ProjectorRenderer;
import li.cil.oc2.client.renderer.color.BusCableBlockColor;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.RobotWithoutLevelRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.client.renderer.fabric.ModShadersFabric;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.Items;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class ClientSetupFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ComputerRenderer.initialize();
        ProjectorDepthRenderer.initialize();
        ModShadersFabric.initialize();
        ClientLevelEventsFabric.initialize();
        ClientRenderEventsFabric.initialize();

        CustomItemModelProperties.initialize();
        CustomItemColors.initialize();

        BusCableModelLoader.initialize();
        ColorHandlerRegistry.registerBlockColors(new BusCableBlockColor(), Blocks.BUS_CABLE);

        BlockEntityRendererRegistry.register(BlockEntities.COMPUTER.get(), ComputerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.FLASH_DRIVE.get(), FlashDriveRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.CHARGER.get(), ChargerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PROJECTOR.get(), ProjectorRenderer::new);

        EntityRendererRegistry.register(Entities.ROBOT, RobotRenderer::new);
        EntityModelLayerRegistry.register(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);

        MenuRegistry.registerScreenFactory(Containers.COMPUTER.get(), ComputerContainerScreen::new);
        MenuRegistry.registerScreenFactory(Containers.COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
        MenuRegistry.registerScreenFactory(Containers.ROBOT.get(), RobotContainerScreen::new);
        MenuRegistry.registerScreenFactory(Containers.ROBOT_TERMINAL.get(), RobotTerminalScreen::new);
        MenuRegistry.registerScreenFactory(Containers.NETWORK_TUNNEL.get(), NetworkTunnelScreen::new);

        BuiltinItemRendererRegistry.INSTANCE.register(Items.ROBOT.get(), new RobotItemRenderer());

        registerBusCablePickBlock();
    }

    // --------------------------------------------------------------------- //

    /**
     * Matches NeoForge's {@code Block#getCloneItemStack}.
     */
    private static void registerBusCablePickBlock() {
        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (!(result instanceof final BlockHitResult hit) || result.getType() != HitResult.Type.BLOCK) {
                return ItemStack.EMPTY;
            }

            if (player.level().getBlockEntity(hit.getBlockPos()) instanceof BusCableBlockEntity) {
                return BusCableBlock.getPickedStack(player.level(), hit.getBlockPos(), player);
            }

            return ItemStack.EMPTY;
        });
    }

    private static final class RobotItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
        private RobotWithoutLevelRenderer renderer;

        @Override
        public void render(final ItemStack stack, final net.minecraft.world.item.ItemDisplayContext context,
                           final com.mojang.blaze3d.vertex.PoseStack poseStack,
                           final net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           final int light, final int overlay) {
            if (renderer == null) {
                final Minecraft minecraft = Minecraft.getInstance();
                renderer = new RobotWithoutLevelRenderer(
                    minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
            renderer.renderByItem(stack, context, poseStack, bufferSource, light, overlay);
        }
    }
}
