package li.cil.oc2.common.block;

import com.google.common.collect.Maps;
import li.cil.oc2.client.gui.BusInterfaceScreen;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tileentity.BusCableBlockEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class BusCableBlock extends BaseEntityBlock {
    public enum ConnectionType implements StringRepresentable {
        NONE,
        CABLE,
        INTERFACE;

        @Override
        public String getSerializedName() {
            switch (this) {
                case NONE:
                    return "none";
                case CABLE:
                    return "cable";
                case INTERFACE:
                    return "interface";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    public static final BooleanProperty HAS_CABLE = BooleanProperty.create("has_cable");
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
            return state.getValue(BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction));
        } else {
            return ConnectionType.NONE;
        }
    }

    public static int getInterfaceCount(final BlockState state) {
        int partCount = 0;
        for (final EnumProperty<ConnectionType> connectionType : FACING_TO_CONNECTION_MAP.values()) {
            if (state.getValue(connectionType) == ConnectionType.INTERFACE) {
                partCount++;
            }
        }
        return partCount;
    }

    public static Direction getHitSide(final BlockPos pos, final BlockHitResult hit) {
        final Vec3 localHitPos = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        return Direction.getNearest(localHitPos.x, localHitPos.y, localHitPos.z);
    }

    ///////////////////////////////////////////////////////////////////

    private final VoxelShape[] shapes;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlock() {
        super(Properties
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(1.5f, 6.0f));

        BlockState defaultState = getStateDefinition().any();
        for (final EnumProperty<ConnectionType> property : FACING_TO_CONNECTION_MAP.values()) {
            defaultState = defaultState.setValue(property, ConnectionType.NONE);
        }
        defaultState = defaultState.setValue(HAS_CABLE, true);
        registerDefaultState(defaultState);

        shapes = makeShapes();
    }

    ///////////////////////////////////////////////////////////////////

    public boolean addInterface(final Level world, final BlockPos pos, final BlockState state, final Direction side) {
        final EnumProperty<BusCableBlock.ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);
        if (state.getValue(property) != ConnectionType.NONE) {
            return false;
        }

        world.setBlock(pos, state.setValue(property, ConnectionType.INTERFACE), Constants.BlockFlags.DEFAULT_AND_RERENDER);

        onConnectionTypeChanged(world, pos, side);

        return true;
    }

    public boolean addCable(final Level world, final BlockPos pos, final BlockState state) {
        if (state.getValue(HAS_CABLE)) {
            return false;
        }

        world.setBlock(pos, state.setValue(HAS_CABLE, true), Constants.BlockFlags.DEFAULT_AND_RERENDER);

        onConnectionTypeChanged(world, pos, null);

        return true;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockGetter world) {
        return TileEntities.BUS_CABLE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final Level world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof BusCableBlockEntity) {
            final BusCableBlockEntity busCable = (BusCableBlockEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(final BlockState state, final Level world, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (Wrenches.isWrench(player.getItemInHand(hand))) {
            if (player.isShiftKeyDown()) {
                // NB: leave wrenching logic up to wrench when the to-be-removed interface is the last
                //     part of this bus. This ensures we properly remove the block itself without having
                //     to duplicate the logic needed for that.
                if (getPartCount(state) > 1)
                    if (tryRemovePlug(state, world, pos, player, hit) || tryRemoveCable(state, world, pos, player)) {
                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
            } else {
                final BlockEntity tileEntity = world.getBlockEntity(pos);
                if (tileEntity instanceof BusCableBlockEntity) {
                    final BusCableBlockEntity busCableBlockEntity = (BusCableBlockEntity) tileEntity;

                    final Direction side = getHitSide(pos, hit);
                    if (getConnectionType(state, side) == ConnectionType.INTERFACE) {
                        openBusInterfaceScreen(busCableBlockEntity, side);
                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
                }
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootContext.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));

        if (state.getValue(HAS_CABLE)) {
            drops.add(new ItemStack(Items.BUS_CABLE));
        }

        int interfaceCount = 0;
        for (final Direction side : Constants.DIRECTIONS) {
            final ConnectionType connectionType = state.getValue(FACING_TO_CONNECTION_MAP.get(side));
            if (connectionType == ConnectionType.INTERFACE) {
                interfaceCount++;
            }
        }

        if (interfaceCount > 0) {
            drops.add(new ItemStack(Items.BUS_INTERFACE, interfaceCount));
        }

        return drops;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState state = defaultBlockState();

        final Level world = context.getLevel();
        final BlockPos position = context.getClickedPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry : FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.relative(facing);
            if (context.getItemInHand().getItem() == Items.BUS_CABLE &&
                canHaveCableTo(world.getBlockState(facingPos), facing.getOpposite())) {
                state = state.setValue(entry.getValue(), ConnectionType.CABLE);
            }
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, final Direction facing, final BlockState facingState, final Level world, final BlockPos currentPos, final BlockPos facingPos) {
        if (state.getValue(FACING_TO_CONNECTION_MAP.get(facing)) == ConnectionType.INTERFACE) {
            return state;
        }

        if (state.getValue(HAS_CABLE) && canHaveCableTo(facingState, facing.getOpposite())) {
            state = state.setValue(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.CABLE);
        } else {
            state = state.setValue(FACING_TO_CONNECTION_MAP.get(facing), ConnectionType.NONE);
        }

        onConnectionTypeChanged(world, currentPos, facing);

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter world, final BlockPos pos, final CollisionContext context) {
        return shapes[getShapeIndex(state)];
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
        builder.add(HAS_CABLE);
    }

    ///////////////////////////////////////////////////////////////////

    private static boolean canHaveCableTo(final BlockState state, final Direction side) {
        return state.getBlock() == Blocks.BUS_CABLE &&
               state.getValue(HAS_CABLE) &&
               state.getValue(FACING_TO_CONNECTION_MAP.get(side)) != ConnectionType.INTERFACE;
    }

    private static int getPartCount(final BlockState state) {
        int partCount = getInterfaceCount(state);
        if (state.getValue(HAS_CABLE)) {
            partCount++;
        }
        return partCount;
    }

    private static boolean tryRemovePlug(final BlockState state, final Level world, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final Direction side = getHitSide(pos, hit);
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);

        if (state.getValue(property) != ConnectionType.INTERFACE) {
            return false;
        }

        final BlockPos neighborPos = pos.relative(side);
        if (state.getValue(HAS_CABLE) && canHaveCableTo(world.getBlockState(neighborPos), side.getOpposite())) {
            world.setBlockAndUpdate(pos, state.setValue(property, ConnectionType.CABLE));
        } else {
            world.setBlockAndUpdate(pos, state.setValue(property, ConnectionType.NONE));
        }

        handlePartRemoved(state, world, pos, side, player, new ItemStack(Items.BUS_INTERFACE));

        return true;
    }

    private static boolean tryRemoveCable(final BlockState state, final Level world, final BlockPos pos, final Player player) {
        if (!state.getValue(HAS_CABLE)) {
            return false;
        }

        world.setBlockAndUpdate(pos, state.setValue(HAS_CABLE, false));

        handlePartRemoved(state, world, pos, null, player, new ItemStack(Items.BUS_CABLE));

        return true;
    }

    private static void handlePartRemoved(final BlockState state, final Level world, final BlockPos pos, @Nullable final Direction side, final Player player, final ItemStack drop) {
        onConnectionTypeChanged(world, pos, side);

        if (!player.isCreative() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemStackUtils.spawnAsEntity(world, pos, drop, side).ifPresent(entity -> {
                entity.setNoPickUpDelay();
                entity.playerTouch(player);
            });
        }

        WorldUtils.playSound(world, pos, state.getSoundType(), SoundType::getBreakSound);
    }

    private static void onConnectionTypeChanged(final Level world, final BlockPos pos, @Nullable final Direction face) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof BusCableBlockEntity) {
            final BusCableBlockEntity busCable = (BusCableBlockEntity) tileEntity;
            busCable.handleConnectivityChanged(face);
        }
    }

    private static void openBusInterfaceScreen(final BusCableBlockEntity tileEntity, final Direction side) {
        final BusInterfaceScreen screen = new BusInterfaceScreen(tileEntity, side);
        Minecraft.getInstance().setScreen(screen);
    }

    private static VoxelShape[] makeShapes() {
        final VoxelShape ownCableBounds = Block.box(5, 5, 5, 11, 11, 11);
        final VoxelShape[] cableShapes = new VoxelShape[Constants.BLOCK_FACE_COUNT];
        final VoxelShape[] interfaceShapes = new VoxelShape[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            cableShapes[i] = getCableShape(Constants.DIRECTIONS[i]);
            interfaceShapes[i] = getInterfaceShape(Constants.DIRECTIONS[i]);
        }

        // We pack info as such:
        // [has interface on side] [has cable on side] [has own cable]
        // Which is a total of 13 bits (6 + 6 + 1), so 8k combinations.
        // It's a lot, but not so much that it's not ok to still cache
        // it and avoid recomputing the bounds all the time.

        final int configurations = 1 << (6 + 6 + 1);
        final VoxelShape[] result = new VoxelShape[configurations];
        Arrays.fill(result, Shapes.empty());

        for (int i = 0; i < result.length; i++) {
            final int mask = i >> 1;
            for (int sideIndex = 0; sideIndex < Constants.BLOCK_FACE_COUNT; sideIndex++) {
                final int cableBit = 1 << sideIndex;
                if ((mask & cableBit) != 0) {
                    result[i] = Shapes.or(result[i], cableShapes[sideIndex]);
                }

                final int interfaceBit = cableBit << 6;
                if ((mask & interfaceBit) != 0) {
                    result[i] = Shapes.or(result[i], interfaceShapes[sideIndex]);
                }
            }

            if ((i & 1) != 0) {
                result[i] = Shapes.or(result[i], ownCableBounds);
            }
        }

        return result;
    }


    private static VoxelShape getCableShape(final Direction zDirection) {
        final int xSize = 6;
        final int ySize = 6;
        final int zSize = 5;

        final Direction yDirection = zDirection.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        final Direction xDirection = zDirection.getAxis() == Direction.Axis.Y ? Direction.WEST : zDirection.getClockWise();

        final Vec3i min = new Vec3i(8, 8, 8)
                .relative(xDirection, -xSize / 2)
                .relative(yDirection, -ySize / 2)
                .relative(zDirection, 8 - zSize);
        final Vec3i max = new Vec3i(8, 8, 8)
                .relative(xDirection, xSize / 2)
                .relative(yDirection, ySize / 2)
                .relative(zDirection, 8);

        final AABB bounds = new AABB(
                Vec3.atLowerCornerOf(min).scale(1 / 16.0),
                Vec3.atLowerCornerOf(max).scale(1 / 16.0)
        );

        return Shapes.create(bounds);
    }

    private static VoxelShape getInterfaceShape(final Direction zDirection) {
        final int xSize = 8;
        final int ySize = 8;
        final int zSize = 1;

        final Direction yDirection = zDirection.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        final Direction xDirection = zDirection.getAxis() == Direction.Axis.Y ? Direction.WEST : zDirection.getClockWise();

        final Vec3i min = new Vec3i(8, 8, 8)
                .relative(xDirection, -xSize / 2)
                .relative(yDirection, -ySize / 2)
                .relative(zDirection, 8 - zSize);
        final Vec3i max = new Vec3i(8, 8, 8)
                .relative(xDirection, xSize / 2)
                .relative(yDirection, ySize / 2)
                .relative(zDirection, 8);

        final AABB bounds = new AABB(
                Vec3.atLowerCornerOf(min).scale(1 / 16.0),
                Vec3.atLowerCornerOf(max).scale(1 / 16.0)
        );

        return Shapes.or(getCableShape(zDirection), Shapes.create(bounds));
    }

    private static int getShapeIndex(final BlockState state) {
        int index = 0;

        for (int sideIndex = 0; sideIndex < Constants.BLOCK_FACE_COUNT; sideIndex++) {
            final int cableBit = 1 << sideIndex;
            final int interfaceBit = cableBit << 6;
            switch (state.getValue(FACING_TO_CONNECTION_MAP.get(Constants.DIRECTIONS[sideIndex]))) {
                case CABLE:
                    index |= cableBit;
                    break;
                case INTERFACE:
                    index |= interfaceBit;
                    break;
            }
        }

        index = index << 1;

        if (state.getValue(HAS_CABLE)) {
            index |= 1;
        }

        return index;
    }
}
