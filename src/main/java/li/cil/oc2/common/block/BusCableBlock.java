package li.cil.oc2.common.block;

import com.google.common.collect.Maps;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.tile.BusCableTileEntity;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
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
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class BusCableBlock extends Block {
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

    private static final Direction[] FACING_VALUES = Direction.values();

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

    private final VoxelShape[] shapes;

    public BusCableBlock() {
        super(Properties.create(Material.IRON).sound(SoundType.METAL));

        BlockState defaultState = getStateContainer().getBaseState();
        for (final EnumProperty<ConnectionType> property : FACING_TO_CONNECTION_MAP.values()) {
            defaultState = defaultState.with(property, ConnectionType.NONE);
        }
        setDefaultState(defaultState);

        shapes = makeShapes();
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return OpenComputers.BUS_CABLE_TILE_ENTITY.get().create();
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
        final ItemStack heldItem = player.getHeldItem(hand);
        if (Block.getBlockFromItem(heldItem.getItem()) == this) {
            return ActionResultType.PASS;
        }

        final Vector3d localHitPos = hit.getHitVec().subtract(Vector3d.copyCentered(pos));
        if (!tryTogglePlug(state, world, pos, Direction.getFacingFromVector(localHitPos.x, localHitPos.y, localHitPos.z))) {
            tryTogglePlug(state, world, pos, hit.getFace());
        }
        return ActionResultType.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        BlockState state = getDefaultState();

        final World world = context.getWorld();
        final BlockPos position = context.getPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry : FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.offset(facing);
            if (canConnectTo(world, facing, world.getBlockState(facingPos), facingPos)) {
                state = state.with(entry.getValue(), ConnectionType.LINK);
            }
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updatePostPlacement(BlockState state, final Direction facing, final BlockState facingState, final IWorld world, final BlockPos currentPos, final BlockPos facingPos) {
        if (canConnectTo(world, facing, facingState, facingPos)) {
            state = state.with(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.LINK);
        } else if (state.get(FACING_TO_CONNECTION_MAP.get(facing)) != ConnectionType.PLUG) {
            state = state.with(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.NONE);
        }

        return state;
    }

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final IBlockReader world, final BlockPos pos, final ISelectionContext context) {
        return shapes[getShapeIndex(state)];
    }

    private VoxelShape[] makeShapes() {
        final VoxelShape coreShape = Block.makeCuboidShape(5, 5, 5, 11, 11, 11);
        final VoxelShape[] connectionShapes = new VoxelShape[FACING_VALUES.length];
        for (int i = 0; i < FACING_VALUES.length; i++) {
            final Direction direction = FACING_VALUES[i];
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

            for (int j = 0; j < FACING_VALUES.length; j++) {
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

        for (int i = 0; i < FACING_VALUES.length; i++) {
            if (state.get(FACING_TO_CONNECTION_MAP.get(FACING_VALUES[i])) != ConnectionType.NONE) {
                index |= 1 << i;
            }
        }

        return index;
    }

    private boolean canConnectTo(final IWorld world, final Direction facing, final BlockState facingState, final BlockPos facingPos) {
        if (facingState.getBlock() == this) {
            return true;
        }

        final TileEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(world, facingPos);
        if (tileEntity == null) {
            return false;
        }

        return tileEntity.getCapability(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, facing.getOpposite()).isPresent();
    }

    private boolean tryTogglePlug(final BlockState state, final World world, final BlockPos pos, final Direction face) {
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(face);
        if (state.get(property) == ConnectionType.NONE) {
            world.setBlockState(pos, state.with(property, ConnectionType.PLUG));
        } else if (state.get(property) == ConnectionType.PLUG) {
            world.setBlockState(pos, state.with(property, ConnectionType.NONE));
        } else {
            return false;
        }

        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleNeighborChanged(pos.offset(face));
        }

        return true;
    }
}
