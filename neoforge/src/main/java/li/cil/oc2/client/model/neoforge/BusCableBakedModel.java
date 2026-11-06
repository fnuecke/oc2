/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.neoforge;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BusCableBakedModel implements DynamicBlockStateModel {
    private final BlockStateModel proxy;
    private final BlockModelPart[] straightModelByAxis;
    private final BlockModelPart[] supportModelByFace;
    private final Map<TextureAtlasSprite, Boolean> opaqueSprites = new ConcurrentHashMap<>();

    // --------------------------------------------------------------------- //

    public BusCableBakedModel(final BlockStateModel proxy, final BlockModelPart[] straightModelByAxis, final BlockModelPart[] supportModelByFace) {
        this.proxy = proxy;
        this.straightModelByAxis = straightModelByAxis;
        this.supportModelByFace = supportModelByFace;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void collectParts(final BlockAndTintGetter level, final BlockPos pos, final BlockState state,
                             final RandomSource random, final List<BlockModelPart> parts) {
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            collectFacadeParts(level, pos, random, parts);
            return;
        }

        if (!state.hasProperty(BusCableBlock.HAS_CABLE) || !state.getValue(BusCableBlock.HAS_CABLE)) {
            return;
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            if (isStraightAlongAxis(state, Constants.AXES[i])) {
                parts.add(straightModelByAxis[i]);
                return;
            }
        }

        proxy.collectParts(level, pos, state, random, parts);

        final Direction supportSide = getSupportSide(level, pos, state);
        if (supportSide != null) {
            parts.add(supportModelByFace[supportSide.get3DDataValue()]);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public TextureAtlasSprite particleIcon() {
        return proxy.particleIcon();
    }

    // --------------------------------------------------------------------- //

    private void collectFacadeParts(final BlockAndTintGetter level, final BlockPos pos,
                                    final RandomSource random, final List<BlockModelPart> parts) {
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
        final List<BlockModelPart> facadeParts = new ArrayList<>();
        model.collectParts(level, pos, facadeState, random, facadeParts);
        for (final BlockModelPart part : facadeParts) {
            final ChunkSectionLayer layer = part.getRenderType(facadeState);
            if (layer == ChunkSectionLayer.SOLID) {
                parts.add(new FacadePart(part, layer));
                continue;
            }

            final QuadCollection.Builder opaqueQuads = new QuadCollection.Builder();
            final QuadCollection.Builder otherQuads = new QuadCollection.Builder();
            addQuads(part, null, opaqueQuads, otherQuads);
            for (final Direction side : Constants.DIRECTIONS) {
                addQuads(part, side, opaqueQuads, otherQuads);
            }
            addPart(parts, part, opaqueQuads.build(), ChunkSectionLayer.SOLID);
            addPart(parts, part, otherQuads.build(), layer);
        }
    }

    private static void addPart(final List<BlockModelPart> parts, final BlockModelPart part, final QuadCollection quads, final ChunkSectionLayer layer) {
        if (!quads.getAll().isEmpty()) {
            parts.add(new SimpleModelWrapper(quads, part.useAmbientOcclusion(), part.particleIcon(), layer));
        }
    }

    private void addQuads(final BlockModelPart part, @Nullable final Direction side,
                          final QuadCollection.Builder opaqueQuads, final QuadCollection.Builder otherQuads) {
        for (final BakedQuad quad : part.getQuads(side)) {
            final QuadCollection.Builder quads = isOpaque(quad.sprite()) ? opaqueQuads : otherQuads;
            if (side == null) {
                quads.addUnculledFace(quad);
            } else {
                quads.addCulledFace(side, quad);
            }
        }
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

    private record FacadePart(BlockModelPart part, ChunkSectionLayer layer) implements BlockModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable final Direction side) {
            return part.getQuads(side);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return part.useAmbientOcclusion();
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return part.particleIcon();
        }

        @Override
        public ChunkSectionLayer getRenderType(final BlockState state) {
            return layer;
        }

        @Override
        public TriState ambientOcclusion() {
            return part.ambientOcclusion();
        }
    }
}
