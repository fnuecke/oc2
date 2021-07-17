package li.cil.oc2.client.model;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.state.EnumProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class BusCableBakedModel implements IDynamicBakedModel {
    private static final ModelProperty<BusCableSupportSide> BUS_CABLE_SUPPORT_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<BusCableFacade> BUS_CABLE_FACADE_PROPERTY = new ModelProperty<>();

    private final IBakedModel proxy;
    private final IBakedModel[] straightModelByAxis;
    private final IBakedModel[] supportModelByFace;

    ///////////////////////////////////////////////////////////////////

    public BusCableBakedModel(final IBakedModel proxy, final IBakedModel[] straightModelByAxis, final IBakedModel[] supportModelByFace) {
        this.proxy = proxy;
        this.straightModelByAxis = straightModelByAxis;
        this.supportModelByFace = supportModelByFace;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, final Random rand, final IModelData extraData) {
        if (extraData.hasProperty(BUS_CABLE_FACADE_PROPERTY)) {
            final BusCableFacade facade = extraData.getData(BUS_CABLE_FACADE_PROPERTY);
            return facade.model.getQuads(facade.blockState, side, rand, facade.data);
        }

        if (state == null || !state.getValue(BusCableBlock.HAS_CABLE)) {
            return Collections.emptyList();
        }

        for (int i = 0; i < Constants.AXES.length; i++) {
            final Direction.Axis axis = Constants.AXES[i];
            if (isStraightAlongAxis(state, axis)) {
                return straightModelByAxis[i].getQuads(state, side, rand, extraData);
            }
        }

        final ArrayList<BakedQuad> quads = new ArrayList<>(proxy.getQuads(state, side, rand, extraData));

        final BusCableSupportSide supportSide = extraData.getData(BUS_CABLE_SUPPORT_PROPERTY);
        if (supportSide != null) {
            quads.addAll(supportModelByFace[supportSide.value.get3DDataValue()].getQuads(state, side, rand, extraData));
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
    public ItemOverrideList getOverrides() {
        return proxy.getOverrides();
    }

    @Override
    @Nonnull
    public IModelData getModelData(final IBlockDisplayReader world, final BlockPos pos, final BlockState state, final IModelData tileData) {
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            final TileEntity tileEntity = world.getBlockEntity(pos);
            final BlockState facadeState;
            if (tileEntity instanceof BusCableTileEntity && ((BusCableTileEntity) tileEntity).hasFacade()) {
                facadeState = ((BusCableTileEntity) tileEntity).getFacade();
            } else {
                facadeState = Blocks.IRON_BLOCK.defaultBlockState();
            }

            final BlockModelShapes shapes = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
            final IBakedModel model = shapes.getBlockModel(facadeState);
            final IModelData data = model.getModelData(world, pos, facadeState, tileData);

            return new ModelDataMap.Builder()
                    .withInitial(BUS_CABLE_FACADE_PROPERTY, new BusCableFacade(facadeState, model, data))
                    .build();
        }

        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (isNeighborInDirectionSolid(world, pos, direction)) {
                final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property) && state.getValue(property) == BusCableBlock.ConnectionType.INTERFACE) {
                    return tileData; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }

        if (supportSide != null) {
            return new ModelDataMap.Builder()
                    .withInitial(BUS_CABLE_SUPPORT_PROPERTY, new BusCableSupportSide(supportSide))
                    .build();
        }

        return tileData;
    }

    ///////////////////////////////////////////////////////////////////

    private static boolean isNeighborInDirectionSolid(final IBlockDisplayReader world, final BlockPos pos, final Direction direction) {
        final BlockPos neighborPos = pos.relative(direction);
        return world.getBlockState(neighborPos).isFaceSturdy(world, neighborPos, direction.getOpposite());
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

    ///////////////////////////////////////////////////////////////////

    private static final class BusCableSupportSide {
        public final Direction value;

        private BusCableSupportSide(final Direction value) {
            this.value = value;
        }
    }

    private static final class BusCableFacade {
        public final BlockState blockState;
        public final IBakedModel model;
        public final IModelData data;

        public BusCableFacade(final BlockState blockState, final IBakedModel model, final IModelData data) {
            this.blockState = blockState;
            this.model = model;
            this.data = data;
        }
    }
}
