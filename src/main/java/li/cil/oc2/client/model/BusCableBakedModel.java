/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BusCableBakedModel(
    BakedModel proxy,
    BakedModel[] straightModelByAxis,
    BakedModel[] supportModelByFace
) implements IDynamicBakedModel {
    private static final ModelProperty<BusCableSupportSide> BUS_CABLE_SUPPORT_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<BusCableFacade> BUS_CABLE_FACADE_PROPERTY = new ModelProperty<>();

    ///////////////////////////////////////////////////////////////////

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

    @Override
    @Nonnull
    public @NotNull List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, @NotNull final RandomSource rand, @NotNull final ModelData extraData, @Nullable final RenderType renderType) {
        if (extraData.has(BUS_CABLE_FACADE_PROPERTY)) {
            final BusCableFacade facade = extraData.get(BUS_CABLE_FACADE_PROPERTY);
            if (facade != null && (renderType == null)) { // || ItemBlockRenderTypes.(facade.blockState, renderType))) {
                return facade.model.getQuads(facade.blockState, side, rand, facade.data, renderType);
            } else {
                return Collections.emptyList();
            }
        }

        if (state == null || !state.getValue(BusCableBlock.HAS_CABLE) || renderType == null || !renderType.equals(RenderType.solid())) {
            return Collections.emptyList();
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            final Direction.Axis axis = Constants.AXES[i];
            if (isStraightAlongAxis(state, axis)) {
                return straightModelByAxis[i].getQuads(state, side, rand, extraData, renderType);
            }
        }

        final ArrayList<BakedQuad> quads = new ArrayList<>(proxy.getQuads(state, side, rand, extraData, renderType));

        final BusCableSupportSide supportSide = extraData.get(BUS_CABLE_SUPPORT_PROPERTY);
        if (supportSide != null) {
            quads.addAll(supportModelByFace[supportSide.value.get3DDataValue()].getQuads(state, side, rand, extraData, renderType));
        }

        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return proxy.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return proxy.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return proxy.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return proxy.isCustomRenderer();
    }

    @SuppressWarnings("deprecation")
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return proxy.getParticleIcon();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ItemOverrides getOverrides() {
        return proxy.getOverrides();
    }

    @Override
    @Nonnull
    public @NotNull ModelData getModelData(@NotNull final BlockAndTintGetter level, @NotNull final BlockPos pos, @NotNull final BlockState state, @NotNull final ModelData modelData) {
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);

            BlockState facadeState = null;
            if (blockEntity instanceof final BusCableBlockEntity busCable) {
                final ItemStack facadeItem = busCable.getFacade();
                facadeState = ItemStackUtils.getBlockState(facadeItem);
            }
            if (facadeState == null) {
                facadeState = Blocks.IRON_BLOCK.defaultBlockState();
            }

            final BlockModelShaper shapes = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
            final BakedModel model = shapes.getBlockModel(facadeState);
            final ModelData data = model.getModelData(level, pos, facadeState, modelData);

            return ModelData.builder()
                .with(BUS_CABLE_FACADE_PROPERTY, new BusCableFacade(facadeState, model, data))
                .build();
        }

        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property) && state.getValue(property) == BusCableBlock.ConnectionType.INTERFACE) {
                    return modelData; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }

        if (supportSide != null) {
            return ModelData.builder()
                .with(BUS_CABLE_SUPPORT_PROPERTY, new BusCableSupportSide(supportSide))
                .build();
        }

        return modelData;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull final BlockState state, @NotNull final RandomSource rand, @NotNull final ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.chunkBufferLayers().toArray(RenderType[]::new));
    }

    ///////////////////////////////////////////////////////////////////

    private record BusCableSupportSide(Direction value) {
    }

    private record BusCableFacade(BlockState blockState, BakedModel model, ModelData data) {
    }
}
