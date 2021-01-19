package li.cil.oc2.common.block;

import com.google.common.collect.Maps;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BusCableBlock extends Block {
    public enum ConnectionType implements IStringSerializable {
        NONE,
        LINK,
        PLUG;

        @Override
        public String getString() {
            switch (this) {
                case NONE:
                    return "none";
                case LINK:
                    return "link";
                case PLUG:
                    return "plug";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    public static final EnumProperty<ConnectionType> CONNECTION_NORTH = EnumProperty.create("connection_north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_EAST = EnumProperty.create("connection_east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_SOUTH = EnumProperty.create("connection_south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_WEST = EnumProperty.create("connection_west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_UP = EnumProperty.create("connection_up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_DOWN = EnumProperty.create("connection_down", ConnectionType.class);

    public static final Map<Direction, EnumProperty<ConnectionType>> FACING_TO_CONNECTION_MAP = Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, CONNECTION_NORTH);
        directions.put(Direction.EAST, CONNECTION_EAST);
        directions.put(Direction.SOUTH, CONNECTION_SOUTH);
        directions.put(Direction.WEST, CONNECTION_WEST);
        directions.put(Direction.UP, CONNECTION_UP);
        directions.put(Direction.DOWN, CONNECTION_DOWN);
    });

    public static ConnectionType getConnectionType(final BlockState state, @Nullable final Direction direction) {
        if (direction != null) {
            return state.get(BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction));
        } else {
            return ConnectionType.NONE;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private final VoxelShape[] shapes;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlock() {
        super(Properties
                .create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(1.5f, 6.0f));

        BlockState defaultState = getStateContainer().getBaseState();
        for (final EnumProperty<ConnectionType> property : FACING_TO_CONNECTION_MAP.values()) {
            defaultState = defaultState.with(property, ConnectionType.NONE);
        }
        setDefaultState(defaultState);

        shapes = makeShapes();
    }

    ///////////////////////////////////////////////////////////////////

    public boolean addPlug(final World world, final BlockPos pos, final BlockState state, final Direction side) {
        final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(side);
        if (state.get(property) == BusCableBlock.ConnectionType.NONE) {
            if (!world.isRemote()) {
                world.setBlockState(pos, state.with(property, ConnectionType.PLUG));
                onConnectionTypeChanged(world, pos, side);
                WorldUtils.playSound(world, pos, state.getSoundType(), SoundType::getPlaceSound);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.BUS_CABLE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final World world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        if (Wrenches.isWrench(player.getHeldItem(hand)) && tryRemovePlug(state, world, pos, player, hit)) {
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootContext.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));

        int plugCount = 0;
        for (final Direction side : Constants.DIRECTIONS) {
            final ConnectionType connectionType = state.get(FACING_TO_CONNECTION_MAP.get(side));
            if (connectionType == ConnectionType.PLUG) {
                plugCount++;
            }
        }

        if (plugCount > 0) {
            drops.add(new ItemStack(Items.BUS_INTERFACE.get(), plugCount));
        }

        return drops;
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        BlockState state = getDefaultState();

        final World world = context.getWorld();
        final BlockPos position = context.getPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry : FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.offset(facing);
            if (isCableBlock(world.getBlockState(facingPos))) {
                state = state.with(entry.getValue(), ConnectionType.LINK);
            }
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updatePostPlacement(BlockState state, final Direction facing, final BlockState facingState, final IWorld world, final BlockPos currentPos, final BlockPos facingPos) {
        if (isCableBlock(facingState)) {
            state = state.with(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.LINK);
        } else if (state.get(FACING_TO_CONNECTION_MAP.get(facing)) != ConnectionType.PLUG) {
            state = state.with(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.NONE);
        }
        onConnectionTypeChanged(world, currentPos, facing);

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final IBlockReader world, final BlockPos pos, final ISelectionContext context) {
        return shapes[getShapeIndex(state)];
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
    }

    ///////////////////////////////////////////////////////////////////

    private VoxelShape[] makeShapes() {
        final VoxelShape coreShape = Block.makeCuboidShape(5, 5, 5, 11, 11, 11);
        final VoxelShape[] connectionShapes = new VoxelShape[Constants.DIRECTIONS.length];
        for (int i = 0; i < Constants.DIRECTIONS.length; i++) {
            final Direction direction = Constants.DIRECTIONS[i];
            connectionShapes[i] = VoxelShapes.create(
                    0.5 + Math.min((-2.0 / (16 - 6)), direction.getXOffset() * 0.5),
                    0.5 + Math.min((-2.0 / (16 - 6)), direction.getYOffset() * 0.5),
                    0.5 + Math.min((-2.0 / (16 - 6)), direction.getZOffset() * 0.5),
                    0.5 + Math.max(2.0 / (16 - 6), direction.getXOffset() * 0.5),
                    0.5 + Math.max(2.0 / (16 - 6), direction.getYOffset() * 0.5),
                    0.5 + Math.max(2.0 / (16 - 6), direction.getZOffset() * 0.5));
        }

        final VoxelShape[] result = new VoxelShape[1 << 6];
        for (int i = 0; i < result.length; i++) {
            VoxelShape shape = coreShape;

            for (int j = 0; j < Constants.DIRECTIONS.length; j++) {
                if ((i & (1 << j)) != 0) {
                    shape = VoxelShapes.or(shape, connectionShapes[j]);
                }
            }

            result[i] = shape;
        }

        return result;
    }

    private int getShapeIndex(final BlockState state) {
        int index = 0;

        for (int i = 0; i < Constants.DIRECTIONS.length; i++) {
            if (state.get(FACING_TO_CONNECTION_MAP.get(Constants.DIRECTIONS[i])) != ConnectionType.NONE) {
                index |= 1 << i;
            }
        }

        return index;
    }

    private boolean isCableBlock(final BlockState state) {
        return state.getBlock() == this;
    }

    private void onConnectionTypeChanged(final IWorld world, final BlockPos pos, final Direction face) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleConnectionTypeChanged(face);
        }
    }

    private boolean tryRemovePlug(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final BlockRayTraceResult hit) {
        final Vector3d localHitPos = hit.getHitVec().subtract(Vector3d.copyCentered(pos));
        final Direction side = Direction.getFacingFromVector(localHitPos.x, localHitPos.y, localHitPos.z);
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);

        if (state.get(property) != ConnectionType.PLUG) {
            return false;
        }

        final BlockPos neighborPos = pos.offset(side);
        if (isCableBlock(world.getBlockState(neighborPos))) {
            world.setBlockState(pos, state.with(property, ConnectionType.LINK));
        } else {
            world.setBlockState(pos, state.with(property, ConnectionType.NONE));
        }
        onConnectionTypeChanged(world, pos, side);

        if (!player.isCreative() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
            ItemStackUtils.spawnAsEntity(world, pos, new ItemStack(Items.BUS_INTERFACE.get()), side).ifPresent(entity -> {
                entity.setNoPickupDelay();
                entity.onCollideWithPlayer(player);
            });
        }

        WorldUtils.playSound(world, pos, state.getSoundType(), SoundType::getBreakSound);

        return true;
    }
}
