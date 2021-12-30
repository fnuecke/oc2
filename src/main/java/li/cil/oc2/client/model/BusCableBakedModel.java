package li.cil.oc2.client.model;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.client.MinecraftForgeClient;
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

    private final BakedModel proxy;
    private final BakedModel[] straightModelByAxis;
    private final BakedModel[] supportModelByFace;

    ///////////////////////////////////////////////////////////////////

    public BusCableBakedModel(final BakedModel proxy, final BakedModel[] straightModelByAxis, final BakedModel[] supportModelByFace) {
        this.proxy = proxy;
        this.straightModelByAxis = straightModelByAxis;
        this.supportModelByFace = supportModelByFace;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, final Random rand, final IModelData extraData) {
        final RenderType layer = MinecraftForgeClient.getRenderType();

        if (extraData.hasProperty(BUS_CABLE_FACADE_PROPERTY)) {
            final BusCableFacade facade = extraData.getData(BUS_CABLE_FACADE_PROPERTY);
            if (layer == null || ItemBlockRenderTypes.canRenderInLayer(facade.blockState, layer)) {
                return facade.model.getQuads(facade.blockState, side, rand, facade.data);
            } else {
                return Collections.emptyList();
            }
        }

        if (state == null || !state.getValue(BusCableBlock.HAS_CABLE) || layer == null || !layer.equals(RenderType.solid())) {
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
    public ItemOverrides getOverrides() {
        return proxy.getOverrides();
    }

    @Override
    @Nonnull
    public IModelData getModelData(final BlockAndTintGetter world, final BlockPos pos, final BlockState state, final IModelData tileData) {
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            final BlockEntity tileEntity = world.getBlockEntity(pos);

            BlockState facadeState = null;
            if (tileEntity instanceof BusCableTileEntity) {
                final ItemStack facadeItem = ((BusCableTileEntity) tileEntity).getFacade();
                facadeState = ItemStackUtils.getBlockState(facadeItem);
            }
            if (facadeState == null) {
                facadeState = Blocks.IRON_BLOCK.defaultBlockState();
            }

            final BlockModelShaper shapes = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
            final BakedModel model = shapes.getBlockModel(facadeState);
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

    private static boolean isNeighborInDirectionSolid(final BlockAndTintGetter world, final BlockPos pos, final Direction direction) {
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
        public final BakedModel model;
        public final IModelData data;

        public BusCableFacade(final BlockState blockState, final BakedModel model, final IModelData data) {
            this.blockState = blockState;
            this.model = model;
            this.data = data;
        }
    }
}
