/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.neoforge;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BusCableBakedModel implements IDynamicBakedModel {
    private static final ModelProperty<BusCableSupportSide> BUS_CABLE_SUPPORT_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<BusCableFacade> BUS_CABLE_FACADE_PROPERTY = new ModelProperty<>();
    private static final ChunkRenderTypeSet CABLE_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.solid());

    // --------------------------------------------------------------------- //

    private final BakedModel proxy;
    private final BakedModel[] straightModelByAxis;
    private final BakedModel[] supportModelByFace;
    private final Map<TextureAtlasSprite, Boolean> opaqueSprites = new ConcurrentHashMap<>();

    // --------------------------------------------------------------------- //

    public BusCableBakedModel(final BakedModel proxy, final BakedModel[] straightModelByAxis, final BakedModel[] supportModelByFace) {
        this.proxy = proxy;
        this.straightModelByAxis = straightModelByAxis;
        this.supportModelByFace = supportModelByFace;
    }

    // --------------------------------------------------------------------- //

    @Override
    public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource rand, final ModelData data) {
        if (data.has(BUS_CABLE_FACADE_PROPERTY)) {
            final BusCableFacade facade = data.get(BUS_CABLE_FACADE_PROPERTY);
            return facade != null
                ? ChunkRenderTypeSet.union(CABLE_RENDER_TYPES, facade.model.getRenderTypes(facade.blockState, rand, facade.data))
                : ChunkRenderTypeSet.none();
        }

        return CABLE_RENDER_TYPES;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, final RandomSource rand, final ModelData extraData, @Nullable final RenderType layer) {
        if (extraData.has(BUS_CABLE_FACADE_PROPERTY)) {
            final BusCableFacade facade = extraData.get(BUS_CABLE_FACADE_PROPERTY);
            return facade != null ? getFacadeQuads(facade, side, rand, layer) : Collections.emptyList();
        }

        if (state == null || !state.getValue(BusCableBlock.HAS_CABLE) || layer == null || !layer.equals(RenderType.solid())) {
            return Collections.emptyList();
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            final Direction.Axis axis = Constants.AXES[i];
            if (isStraightAlongAxis(state, axis)) {
                return straightModelByAxis[i].getQuads(state, side, rand, extraData, layer);
            }
        }

        final ArrayList<BakedQuad> quads = new ArrayList<>(proxy.getQuads(state, side, rand, extraData, layer));

        final BusCableSupportSide supportSide = extraData.get(BUS_CABLE_SUPPORT_PROPERTY);
        if (supportSide != null) {
            quads.addAll(supportModelByFace[supportSide.value.get3DDataValue()].getQuads(state, side, rand, extraData, layer));
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

    @Override
    public ItemOverrides getOverrides() {
        return proxy.getOverrides();
    }

    @Override
    public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final ModelData blockEntityData) {
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
            final ModelData data = model.getModelData(level, pos, facadeState, blockEntityData);

            return ModelData.of(BUS_CABLE_FACADE_PROPERTY, new BusCableFacade(facadeState, model, data));
        }

        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property) && state.getValue(property) == BusCableBlock.ConnectionType.INTERFACE) {
                    return blockEntityData; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }

        if (supportSide != null) {
            return ModelData.of(BUS_CABLE_SUPPORT_PROPERTY, new BusCableSupportSide(supportSide));
        }

        return blockEntityData;
    }

    // --------------------------------------------------------------------- //

    private List<BakedQuad> getFacadeQuads(final BusCableFacade facade, @Nullable final Direction side, final RandomSource rand, @Nullable final RenderType layer) {
        if (layer == null) {
            return facade.model.getQuads(facade.blockState, side, rand, facade.data, null);
        }

        final long seed = rand.nextLong();
        rand.setSeed(seed);
        final ChunkRenderTypeSet facadeLayers = facade.model.getRenderTypes(facade.blockState, rand, facade.data);
        if (facadeLayers.asList().equals(CABLE_RENDER_TYPES.asList())) {
            rand.setSeed(seed);
            return layer.equals(RenderType.solid()) ? facade.model.getQuads(facade.blockState, side, rand, facade.data, layer) : Collections.emptyList();
        }

        final ArrayList<BakedQuad> quads = new ArrayList<>();
        for (final RenderType facadeLayer : facadeLayers) {
            rand.setSeed(seed);
            for (final BakedQuad quad : facade.model.getQuads(facade.blockState, side, rand, facade.data, facadeLayer)) {
                final RenderType quadLayer = facadeLayer.equals(RenderType.solid()) || isOpaque(quad.getSprite()) ? RenderType.solid() : facadeLayer;
                if (quadLayer.equals(layer)) {
                    quads.add(quad);
                }
            }
        }
        return quads;
    }

    private boolean isOpaque(final TextureAtlasSprite sprite) {
        return opaqueSprites.computeIfAbsent(sprite, s -> {
            final SpriteContents contents = s.contents();
            return contents.getUniqueFrames().allMatch(frame -> {
                for (int y = 0; y < contents.height(); y++) {
                    for (int x = 0; x < contents.width(); x++) {
                        if (contents.isTransparent(frame, x, y)) {
                            return false;
                        }
                    }
                }
                return true;
            });
        });
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

    // --------------------------------------------------------------------- //

    private record BusCableSupportSide(Direction value) {
    }

    private record BusCableFacade(BlockState blockState, BakedModel model, ModelData data) {
    }
}
