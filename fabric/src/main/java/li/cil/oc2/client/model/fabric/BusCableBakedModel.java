/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public record BusCableBakedModel(
        BakedModel proxy,
        BakedModel[] straightModelByAxis,
        BakedModel[] supportModelByFace
) implements BakedModel, FabricBakedModel {
    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(final BlockAndTintGetter level, final BlockState state, final BlockPos pos,
                               final Supplier<RandomSource> randomSupplier, final RenderContext context) {
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            emitFacadeQuads(level, pos, randomSupplier, context);
            return;
        }

        if (!state.getValue(BusCableBlock.HAS_CABLE)) {
            return;
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            if (isStraightAlongAxis(state, Constants.AXES[i])) {
                straightModelByAxis[i].emitBlockQuads(level, state, pos, randomSupplier, context);
                return;
            }
        }

        proxy.emitBlockQuads(level, state, pos, randomSupplier, context);

        final Direction supportSide = getSupportSide(level, pos, state);
        if (supportSide != null) {
            supportModelByFace[supportSide.get3DDataValue()].emitBlockQuads(level, state, pos, randomSupplier, context);
        }
    }

    @Override
    public void emitItemQuads(final ItemStack stack, final Supplier<RandomSource> randomSupplier, final RenderContext context) {
        proxy.emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, final RandomSource random) {
        if (state == null || !state.getValue(BusCableBlock.HAS_CABLE)) {
            return Collections.emptyList();
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            if (isStraightAlongAxis(state, Constants.AXES[i])) {
                return straightModelByAxis[i].getQuads(state, side, random);
            }
        }

        return proxy.getQuads(state, side, random);
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

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return proxy.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return proxy.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return proxy.getOverrides();
    }

    // ------------------------------------------------------------- //

    private static void emitFacadeQuads(final BlockAndTintGetter level, final BlockPos pos,
                                        final Supplier<RandomSource> randomSupplier, final RenderContext context) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);

        BlockState facadeState = null;
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            final ItemStack facadeItem = busCable.getFacade();
            facadeState = ItemStackUtils.getBlockState(facadeItem);
        }
        if (facadeState == null) {
            facadeState = Blocks.IRON_BLOCK.defaultBlockState();
        }

        final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(facadeState);

        final RenderMaterial material = facadeMaterial(facadeState);
        if (material != null) {
            context.pushTransform(quad -> {
                quad.material(material);
                return true;
            });
        }

        try {
            model.emitBlockQuads(level, facadeState, pos, randomSupplier, context);
        } finally {
            if (material != null) {
                context.popTransform();
            }
        }
    }

    @Nullable
    private static RenderMaterial facadeMaterial(final BlockState facadeState) {
        final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            return null;
        }

        final BlendMode blendMode = BlendMode.fromRenderLayer(ItemBlockRenderTypes.getChunkRenderType(facadeState));
        return renderer.materialFinder().blendMode(blendMode).find();
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
