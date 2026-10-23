/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public record BusCableBakedModel(
    BlockStateModel proxy,
    BlockModelPart[] straightModelByAxis,
    BlockModelPart[] supportModelByFace
) implements BlockStateModel, FabricBlockStateModel {
    @Override
    public void emitQuads(final QuadEmitter emitter, final BlockAndTintGetter level, final BlockPos pos,
                          final BlockState state, final RandomSource random, final Predicate<Direction> cullTest) {
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            emitFacadeQuads(emitter, level, pos, random, cullTest);
            return;
        }

        if (!state.getValue(BusCableBlock.HAS_CABLE)) {
            return;
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            if (isStraightAlongAxis(state, Constants.AXES[i])) {
                straightModelByAxis[i].emitQuads(emitter, cullTest);
                return;
            }
        }

        proxy.emitQuads(emitter, level, pos, state, random, cullTest);

        final Direction supportSide = getSupportSide(level, pos, state);
        if (supportSide != null) {
            supportModelByFace[supportSide.get3DDataValue()].emitQuads(emitter, cullTest);
        }
    }

    @Override
    public void collectParts(final RandomSource random, final List<BlockModelPart> parts) {
        proxy.collectParts(random, parts);
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return proxy.particleIcon();
    }

    // --------------------------------------------------------------------- //

    private static void emitFacadeQuads(final QuadEmitter emitter, final BlockAndTintGetter level, final BlockPos pos,
                                        final RandomSource random, final Predicate<Direction> cullTest) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);

        BlockState facadeState = null;
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            final ItemStack facadeItem = busCable.getFacade();
            facadeState = ItemStackUtils.getBlockState(facadeItem);
        }
        if (facadeState == null) {
            facadeState = Blocks.IRON_BLOCK.defaultBlockState();
        }

        final BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(facadeState);

        final ChunkSectionLayer layer = ItemBlockRenderTypes.getChunkRenderType(facadeState);
        emitter.pushTransform(quad -> {
            quad.renderLayer(layer);
            return true;
        });

        try {
            model.emitQuads(emitter, level, pos, facadeState, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    @Nullable
    private static Direction getSupportSide(final BlockAndTintGetter level, final BlockPos pos, final BlockState state) {
        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property) && state.getValue(property) == BusCableBlock.ConnectionType.INTERFACE) {
                    return null; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }

        return supportSide;
    }

    private static boolean isNeighborInDirectionSolid(final BlockAndTintGetter level, final BlockPos pos, final Direction direction) {
        final BlockPos neighborPos = pos.relative(direction);
        return level.getBlockState(neighborPos).isFaceSturdy(level, neighborPos, direction.getOpposite());
    }

    private static boolean isStraightAlongAxis(final BlockState state, final Direction.Axis axis) {
        for (final Direction direction : Constants.DIRECTIONS) {
            final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
            if (axis.test(direction)) {
                if (state.getValue(property) != BusCableBlock.ConnectionType.CABLE) {
                    return false;
                }
            } else {
                if (state.getValue(property) != BusCableBlock.ConnectionType.NONE) {
                    return false;
                }
            }
        }

        return true;
    }
}
