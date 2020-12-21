package li.cil.oc2.common.block;

import alexiil.mc.lib.attributes.SearchOptions;
import com.google.common.collect.Maps;
import li.cil.oc2.common.block.entity.BusCableTileEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.init.TileEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class BusCableBlock extends BlockWithEntity {
    public enum ConnectionType implements StringIdentifiable {
        NONE,
        LINK,
        PLUG;

        @Override
        public String asString() {
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

    private static final Direction[] FACING_VALUES = Direction.values();

    public static final EnumProperty<ConnectionType> CONNECTION_NORTH = EnumProperty.of("connection_north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_EAST = EnumProperty.of("connection_east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_SOUTH = EnumProperty.of("connection_south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_WEST = EnumProperty.of("connection_west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_UP = EnumProperty.of("connection_up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_DOWN = EnumProperty.of("connection_down", ConnectionType.class);

    public static final Map<Direction, EnumProperty<ConnectionType>> FACING_TO_CONNECTION_MAP = Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, CONNECTION_NORTH);
        directions.put(Direction.EAST, CONNECTION_EAST);
        directions.put(Direction.SOUTH, CONNECTION_SOUTH);
        directions.put(Direction.WEST, CONNECTION_WEST);
        directions.put(Direction.UP, CONNECTION_UP);
        directions.put(Direction.DOWN, CONNECTION_DOWN);
    });

    ///////////////////////////////////////////////////////////////////

    private final VoxelShape[] shapes;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlock() {
        super(Settings.of(Material.METAL).sounds(BlockSoundGroup.METAL));

        BlockState defaultState = getStateManager().getDefaultState();
        for (final EnumProperty<ConnectionType> property : FACING_TO_CONNECTION_MAP.values()) {
            defaultState = defaultState.with(property, ConnectionType.NONE);
        }
        setDefaultState(defaultState);

        shapes = makeShapes();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(final BlockView world) {
        return TileEntities.BUS_CABLE_TILE_ENTITY.instantiate();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborUpdate(final BlockState state, final World world, final BlockPos pos, final Block block, final BlockPos changedBlockPos, final boolean notify) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockHitResult hit) {
        final ItemStack heldItem = player.getStackInHand(hand);
        if (Block.getBlockFromItem(heldItem.getItem()) == this) {
            return ActionResult.PASS;
        }

        final Vec3d localHitPos = hit.getPos().subtract(Vec3d.ofCenter(pos));
        if (!tryTogglePlug(state, world, pos, Direction.getFacing(localHitPos.x, localHitPos.y, localHitPos.z))) {
            tryTogglePlug(state, world, pos, hit.getSide());
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(final ItemPlacementContext context) {
        BlockState state = getDefaultState();

        final World world = context.getWorld();
        final BlockPos position = context.getBlockPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry : FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.offset(facing);
            if (canConnectTo(world, position, facing, world.getBlockState(facingPos))) {
                state = state.with(entry.getValue(), ConnectionType.LINK);
            }
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, final Direction facing, final BlockState facingState, final WorldAccess world, final BlockPos pos, final BlockPos facingPos) {
        if (world instanceof World && canConnectTo((World) world, pos, facing, facingState)) {
            state = state.with(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.LINK);
        } else if (state.get(FACING_TO_CONNECTION_MAP.get(facing)) != ConnectionType.PLUG) {
            state = state.with(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.NONE);
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockView world, final BlockPos pos, final ShapeContext context) {
        return shapes[getShapeIndex(state)];
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
    }

    ///////////////////////////////////////////////////////////////////

    private VoxelShape[] makeShapes() {
        final VoxelShape coreShape = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
        final VoxelShape[] connectionShapes = new VoxelShape[FACING_VALUES.length];
        for (int i = 0; i < FACING_VALUES.length; i++) {
            final Direction direction = FACING_VALUES[i];
            connectionShapes[i] = VoxelShapes.cuboid(
                    0.5 + Math.min((-2.0 / (16 - 6)), direction.getOffsetX() * 0.5),
                    0.5 + Math.min((-2.0 / (16 - 6)), direction.getOffsetY() * 0.5),
                    0.5 + Math.min((-2.0 / (16 - 6)), direction.getOffsetZ() * 0.5),
                    0.5 + Math.max(2.0 / (16 - 6), direction.getOffsetX() * 0.5),
                    0.5 + Math.max(2.0 / (16 - 6), direction.getOffsetY() * 0.5),
                    0.5 + Math.max(2.0 / (16 - 6), direction.getOffsetZ() * 0.5));
        }

        final VoxelShape[] result = new VoxelShape[1 << 6];
        for (int i = 0; i < result.length; i++) {
            VoxelShape shape = coreShape;

            for (int j = 0; j < FACING_VALUES.length; j++) {
                if ((i & (1 << j)) != 0) {
                    shape = VoxelShapes.union(shape, connectionShapes[j]);
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

    private boolean canConnectTo(final World world, final BlockPos pos, final Direction facing, final BlockState facingState) {
        if (facingState.getBlock() == this) {
            return true;
        }

        return Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY
                .getAll(world, pos, SearchOptions.inDirection(facing))
                .hasOfferedAny();
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

        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleNeighborChanged(pos.offset(face));
        }

        return true;
    }
}
