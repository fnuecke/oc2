package li.cil.oc2.common.block;

import com.google.common.collect.Maps;
import li.cil.oc2.client.gui.BusInterfaceScreen;
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
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.GameRules;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.BlockFlags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class BusCableBlock extends Block {
    public enum ConnectionType implements IStringSerializable {
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
    public static final BooleanProperty HAS_FACADE = BooleanProperty.create("has_facade");
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

    public static Direction getHitSide(final BlockPos pos, final BlockRayTraceResult hit) {
        final Vector3d localHitPos = hit.getLocation().subtract(Vector3d.atCenterOf(pos));
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
        defaultState = defaultState.setValue(HAS_FACADE, false);
        registerDefaultState(defaultState);

        shapes = makeShapes();
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean addInterface(final World world, final BlockPos pos, final BlockState state, final Direction side) {
        if (state.getBlock() != Blocks.BUS_CABLE.get()) {
            return false;
        }

        if (state.getValue(HAS_FACADE)) {
            return false;
        }

        final EnumProperty<BusCableBlock.ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);
        if (state.getValue(property) != ConnectionType.NONE) {
            return false;
        }

        world.setBlock(pos, state.setValue(property, ConnectionType.INTERFACE), BlockFlags.DEFAULT_AND_RERENDER);

        onConnectionTypeChanged(world, pos, side);

        return true;
    }

    public static boolean addCable(final World world, final BlockPos pos, final BlockState state) {
        if (state.getBlock() != Blocks.BUS_CABLE.get()) {
            return false;
        }

        if (state.getValue(HAS_CABLE)) {
            return false;
        }

        world.setBlock(pos, state.setValue(HAS_CABLE, true), BlockFlags.DEFAULT_AND_RERENDER);

        onConnectionTypeChanged(world, pos, null);

        return true;
    }

    public static void setHasFacade(final World world, final BlockPos pos, final BlockState state, @Nullable final BlockState facadeState, final boolean value) {
        if (state.getValue(HAS_FACADE) == value) {
            return;
        }

        world.setBlock(pos, state.setValue(HAS_FACADE, value), BlockFlags.DEFAULT_AND_RERENDER);

        final BlockState soundsSource = facadeState != null ? facadeState : state;
        WorldUtils.playSound(world, pos, soundsSource.getSoundType(), value ? SoundType::getPlaceSound : SoundType::getBreakSound);
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.BUS_CABLE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final World world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        final ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() == Items.BUS_CABLE.get() ||
            heldItem.getItem() == Items.BUS_INTERFACE.get()) {
            return ActionResultType.PASS;
        }

        final TileEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof BusCableTileEntity)) {
            return super.use(state, world, pos, player, hand, hit);
        }

        final BusCableTileEntity busCableTileEntity = (BusCableTileEntity) tileEntity;

        if (Wrenches.isWrench(heldItem)) {
            if (player.isShiftKeyDown()) {
                final ItemStack facadeItem = busCableTileEntity.getFacade();
                if (!facadeItem.isEmpty()) {
                    if (!world.isClientSide()) {
                        busCableTileEntity.removeFacade();
                        if (!player.isCreative() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                            ItemStackUtils.spawnAsEntity(world, pos, facadeItem, hit.getDirection()).ifPresent(entity -> {
                                entity.setNoPickUpDelay();
                                entity.playerTouch(player);
                            });
                        }
                    }
                    return ActionResultType.sidedSuccess(world.isClientSide());
                } else {
                    // NB: leave wrenching logic up to wrench when the to-be-removed interface is the last
                    //     part of this bus. This ensures we properly remove the block itself without having
                    //     to duplicate the logic needed for that.
                    if (getPartCount(state) > 1 && (tryRemovePlug(state, world, pos, player, hit) || tryRemoveCable(state, world, pos, player))) {
                        return ActionResultType.sidedSuccess(world.isClientSide());
                    }
                }
            } else {
                final Direction side = getHitSide(pos, hit);
                if (getConnectionType(state, side) == ConnectionType.INTERFACE) {
                    openBusInterfaceScreen(busCableTileEntity, side);
                    return ActionResultType.sidedSuccess(world.isClientSide());
                }
            }
        } else if (!player.isShiftKeyDown() && !state.getValue(HAS_FACADE) && getInterfaceCount(state) == 0) {
            switch (busCableTileEntity.getFacadeType(heldItem)) {
                case INVALID_BLOCK:
                    if (!world.isClientSide()) {
                        player.displayClientMessage(text("message.{mod}.invalid_facade_block"), true);
                    }

                    // Always return success (even on failure) to avoid accidentally placing blocks.
                    return ActionResultType.sidedSuccess(world.isClientSide());
                case VALID_BLOCK:
                    if (!world.isClientSide()) {
                        busCableTileEntity.setFacade(heldItem);
                        if (!player.abilities.instabuild) {
                            heldItem.shrink(1);
                        }
                    }

                    return ActionResultType.sidedSuccess(world.isClientSide());
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootContext.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));

        if (state.getValue(HAS_FACADE)) {
            final TileEntity tileEntity = builder.getOptionalParameter(LootParameters.BLOCK_ENTITY);
            if (tileEntity instanceof BusCableTileEntity) {
                final ItemStack stack = ((BusCableTileEntity) tileEntity).getFacade();
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }

        if (state.getValue(HAS_CABLE)) {
            drops.add(new ItemStack(Items.BUS_CABLE.get()));
        }

        int interfaceCount = 0;
        for (final Direction side : Constants.DIRECTIONS) {
            final ConnectionType connectionType = state.getValue(FACING_TO_CONNECTION_MAP.get(side));
            if (connectionType == ConnectionType.INTERFACE) {
                interfaceCount++;
            }
        }

        if (interfaceCount > 0) {
            drops.add(new ItemStack(Items.BUS_INTERFACE.get(), interfaceCount));
        }

        return drops;
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        BlockState state = defaultBlockState();

        final World world = context.getLevel();
        final BlockPos position = context.getClickedPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry : FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.relative(facing);
            if (context.getItemInHand().getItem() == Items.BUS_CABLE.get() &&
                canHaveCableTo(world.getBlockState(facingPos), facing.getOpposite())) {
                state = state.setValue(entry.getValue(), ConnectionType.CABLE);
            }
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, final Direction facing, final BlockState facingState, final IWorld world, final BlockPos currentPos, final BlockPos facingPos) {
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
    public VoxelShape getShape(final BlockState state, final IBlockReader world, final BlockPos pos, final ISelectionContext context) {
        if (state.getValue(HAS_FACADE)) {
            return VoxelShapes.block();
        }

        return shapes[getShapeIndex(state)];
    }

    @Override
    public ItemStack getPickBlock(final BlockState state, final RayTraceResult target, final IBlockReader world, final BlockPos pos, final PlayerEntity player) {
        final TileEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof BusCableTileEntity)) {
            return super.getPickBlock(state, target, world, pos, player);
        }

        final BusCableTileEntity busCableTileEntity = (BusCableTileEntity) tileEntity;

        final ItemStack facadeItem = busCableTileEntity.getFacade();
        if (!facadeItem.isEmpty()) {
            return facadeItem;
        }

        if (target instanceof BlockRayTraceResult) {
            final Direction side = getHitSide(pos, (BlockRayTraceResult) target);
            if (getConnectionType(state, side) == ConnectionType.INTERFACE) {
                return new ItemStack(Items.BUS_INTERFACE.get());
            }
        }

        return super.getPickBlock(state, target, world, pos, player);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
        builder.add(HAS_CABLE);
        builder.add(HAS_FACADE);
    }

    ///////////////////////////////////////////////////////////////////

    private static boolean canHaveCableTo(final BlockState state, final Direction side) {
        return state.getBlock() == Blocks.BUS_CABLE.get() &&
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

    private static boolean tryRemovePlug(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final BlockRayTraceResult hit) {
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

        handlePartRemoved(state, world, pos, side, player, new ItemStack(Items.BUS_INTERFACE.get()));

        return true;
    }

    private static boolean tryRemoveCable(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player) {
        if (!state.getValue(HAS_CABLE)) {
            return false;
        }

        world.setBlockAndUpdate(pos, state.setValue(HAS_CABLE, false));

        handlePartRemoved(state, world, pos, null, player, new ItemStack(Items.BUS_CABLE.get()));

        return true;
    }

    private static void handlePartRemoved(final BlockState state, final World world, final BlockPos pos, @Nullable final Direction side, final PlayerEntity player, final ItemStack drop) {
        onConnectionTypeChanged(world, pos, side);

        if (!player.isCreative() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemStackUtils.spawnAsEntity(world, pos, drop, side).ifPresent(entity -> {
                entity.setNoPickUpDelay();
                entity.playerTouch(player);
            });
        }

        WorldUtils.playSound(world, pos, state.getSoundType(), SoundType::getBreakSound);
    }

    private static void onConnectionTypeChanged(final IWorld world, final BlockPos pos, @Nullable final Direction face) {
        final TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleConnectivityChanged(face);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void openBusInterfaceScreen(final BusCableTileEntity tileEntity, final Direction side) {
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
        Arrays.fill(result, VoxelShapes.empty());

        for (int i = 0; i < result.length; i++) {
            final int mask = i >> 1;
            for (int sideIndex = 0; sideIndex < Constants.BLOCK_FACE_COUNT; sideIndex++) {
                final int cableBit = 1 << sideIndex;
                if ((mask & cableBit) != 0) {
                    result[i] = VoxelShapes.or(result[i], cableShapes[sideIndex]);
                }

                final int interfaceBit = cableBit << 6;
                if ((mask & interfaceBit) != 0) {
                    result[i] = VoxelShapes.or(result[i], interfaceShapes[sideIndex]);
                }
            }

            if ((i & 1) != 0) {
                result[i] = VoxelShapes.or(result[i], ownCableBounds);
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

        final Vector3i min = new Vector3i(8, 8, 8)
                .relative(xDirection, -xSize / 2)
                .relative(yDirection, -ySize / 2)
                .relative(zDirection, 8 - zSize);
        final Vector3i max = new Vector3i(8, 8, 8)
                .relative(xDirection, xSize / 2)
                .relative(yDirection, ySize / 2)
                .relative(zDirection, 8);

        final AxisAlignedBB bounds = new AxisAlignedBB(
                Vector3d.atLowerCornerOf(min).scale(1 / 16.0),
                Vector3d.atLowerCornerOf(max).scale(1 / 16.0)
        );

        return VoxelShapes.create(bounds);
    }

    private static VoxelShape getInterfaceShape(final Direction zDirection) {
        final int xSize = 8;
        final int ySize = 8;
        final int zSize = 1;

        final Direction yDirection = zDirection.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        final Direction xDirection = zDirection.getAxis() == Direction.Axis.Y ? Direction.WEST : zDirection.getClockWise();

        final Vector3i min = new Vector3i(8, 8, 8)
                .relative(xDirection, -xSize / 2)
                .relative(yDirection, -ySize / 2)
                .relative(zDirection, 8 - zSize);
        final Vector3i max = new Vector3i(8, 8, 8)
                .relative(xDirection, xSize / 2)
                .relative(yDirection, ySize / 2)
                .relative(zDirection, 8);

        final AxisAlignedBB bounds = new AxisAlignedBB(
                Vector3d.atLowerCornerOf(min).scale(1 / 16.0),
                Vector3d.atLowerCornerOf(max).scale(1 / 16.0)
        );

        return VoxelShapes.or(getCableShape(zDirection), VoxelShapes.create(bounds));
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
