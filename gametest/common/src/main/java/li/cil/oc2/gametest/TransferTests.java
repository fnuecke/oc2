/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.bus.device.item.CraftingModuleDevice;
import li.cil.oc2.common.container.ContainerFluidHandler;
import li.cil.oc2.common.container.ItemHandlerUtils;
import li.cil.oc2.common.container.ItemStackHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidHandlerUtils;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.fluid.FluidTank;
import li.cil.oc2.common.fluid.WorldFluidHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.inventory.WorldItemHandler;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;
import li.cil.oc2.gametest.fixture.DiskDriveFixture;
import li.cil.oc2.gametest.fixture.RobotFixture;
import li.cil.oc2.gametest.util.Drops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.util.TestSupport.ROBOT_POS;
import static li.cil.oc2.gametest.util.TestSupport.abort;
import static li.cil.oc2.gametest.util.TestSupport.assertEquals;
import static li.cil.oc2.gametest.util.TestSupport.assertTrue;
import static li.cil.oc2.gametest.util.TestSupport.assertValue;
import static net.minecraft.world.item.Items.BUCKET;
import static net.minecraft.world.item.Items.DIRT;
import static net.minecraft.world.item.Items.GLASS_BOTTLE;
import static net.minecraft.world.item.Items.HONEY_BLOCK;
import static net.minecraft.world.item.Items.HONEY_BOTTLE;

public final class TransferTests {
    public static void itemTransferMovesWhatFits(final GameTestHelper helper) {
        final ItemStackHandler source = inventory(new ItemStack(DIRT, 10));
        final ItemStackHandler target = inventory(new ItemStack(DIRT, 60));

        assertEquals(helper, "items moved", 4, Transaction.run(transaction -> ItemHandlerUtils.transfer(source, 0, target, 0, 10, transaction)));
        assertEquals(helper, "source after the move", 6, source.getStackInSlot(0).getCount());
        assertEquals(helper, "target after the move", 64, target.getStackInSlot(0).getCount());
        helper.succeed();
    }

    public static void itemTransferWithRemainderChangesNothing(final GameTestHelper helper) {
        final ItemStackHandler source = inventory(new ItemStack(DIRT, 10));
        final ItemStackHandler target = new ItemStackHandler(1) {
            private int inserts;

            @Override
            public ItemStack insertItem(final int slot, final ItemStack stack, final TransactionContext transaction) {
                // Accepts the probe, rejects the actual move.
                return ++inserts > 1 ? stack : super.insertItem(slot, stack, transaction);
            }
        };

        assertEquals(helper, "items moved", 0, Transaction.run(transaction -> ItemHandlerUtils.transfer(source, 0, target, 0, 10, transaction)));
        assertEquals(helper, "source after the failed move", 10, source.getStackInSlot(0).getCount());
        assertTrue(helper, "target after the failed move is empty", target.getStackInSlot(0).isEmpty());
        helper.succeed();
    }

    public static void fluidTransferWithRemainderChangesNothing(final GameTestHelper helper) {
        final FluidTank source = new FluidTank(FluidHandler.BUCKET);
        Transaction.run(transaction -> source.fill(water(), transaction));
        final FluidTank tank = new FluidTank(FluidHandler.BUCKET);
        final FluidHandler target = new FluidHandler() {
            private int fills;

            @Override
            public int getTanks() {
                return tank.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(final int index) {
                return tank.getFluidInTank(index);
            }

            @Override
            public int getTankCapacity(final int index) {
                return tank.getTankCapacity(index);
            }

            @Override
            public int fill(final FluidStack stack, final TransactionContext transaction) {
                // Accepts the probe, rejects the actual move.
                return ++fills > 1 ? 0 : tank.fill(stack, transaction);
            }

            @Override
            public FluidStack drain(final FluidStack stack, final TransactionContext transaction) {
                return tank.drain(stack, transaction);
            }

            @Override
            public FluidStack drain(final int amount, final TransactionContext transaction) {
                return tank.drain(amount, transaction);
            }
        };

        assertEquals(helper, "fluid moved", 0, Transaction.run(transaction -> FluidHandlerUtils.transfer(source, target, FluidHandler.BUCKET, transaction)));
        assertEquals(helper, "source after the failed move", FluidHandler.BUCKET, source.getFluid().amount());
        assertTrue(helper, "target after the failed move is empty", tank.getFluid().isEmpty());
        helper.succeed();
    }

    public static void worldItemHandlerAppliesOnCommit(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(DIRT, 10));
        helper.getLevel().addFreshEntity(entity);
        final ItemHandler world = requireNonNull(WorldItemHandler.of(helper.getLevel(), pos));

        assertEquals(helper, "uncommitted extract", 5, abort(transaction -> world.extractItem(0, 5, transaction)).getCount());
        assertEquals(helper, "item entity after the uncommitted extract", 10, entity.getItem().getCount());
        assertEquals(helper, "committed extract", 5, Transaction.run(transaction -> world.extractItem(0, 5, transaction)).getCount());
        assertEquals(helper, "item entity after the committed extract", 5, entity.getItem().getCount());

        abort(transaction -> world.insertItem(0, new ItemStack(BUCKET), transaction));
        assertEquals(helper, "buckets after the uncommitted insert", 0, itemEntities(helper, pos, new ItemStack(BUCKET)).size());
        Transaction.run(transaction -> world.insertItem(0, new ItemStack(BUCKET), transaction));
        assertEquals(helper, "buckets after the committed insert", 1, itemEntities(helper, pos, new ItemStack(BUCKET)).size());
        helper.succeed();
    }

    public static void worldFluidHandlerAppliesOnCommit(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.WATER);
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final FluidHandler world = requireNonNull(WorldFluidHandler.of(helper.getLevel(), pos));
        final FluidTank tank = new FluidTank(FluidHandler.BUCKET);

        assertEquals(helper, "uncommitted move", FluidHandler.BUCKET, abort(transaction -> FluidHandlerUtils.transfer(world, tank, FluidHandler.BUCKET, transaction)));
        assertTrue(helper, "water source after the uncommitted move", helper.getLevel().getFluidState(pos).isSource());
        assertTrue(helper, "tank after the uncommitted move is empty", tank.getFluid().isEmpty());

        assertEquals(helper, "committed move", FluidHandler.BUCKET, Transaction.run(transaction -> FluidHandlerUtils.transfer(world, tank, FluidHandler.BUCKET, transaction)));
        assertTrue(helper, "water source after the committed move is gone", helper.getLevel().getFluidState(pos).isEmpty());
        assertEquals(helper, "tank after the committed move", FluidHandler.BUCKET, tank.getFluid().amount());

        assertEquals(helper, "committed move back", FluidHandler.BUCKET, Transaction.run(transaction -> FluidHandlerUtils.transfer(tank, world, FluidHandler.BUCKET, transaction)));
        assertTrue(helper, "water source after the move back", helper.getLevel().getFluidState(pos).isSource());
        assertTrue(helper, "tank after the move back is empty", tank.getFluid().isEmpty());
        helper.succeed();
    }

    public static void worldFluidHandlerReplacesDrainedFluid(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.WATER);
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final FluidHandler world = requireNonNull(WorldFluidHandler.of(helper.getLevel(), pos));

        Transaction.run(transaction -> {
            assertEquals(helper, "first drain", FluidHandler.BUCKET, world.drain(FluidHandler.BUCKET, transaction).amount());
            assertEquals(helper, "fill with lava after the drain", FluidHandler.BUCKET, world.fill(lava(), transaction));
            assertTrue(helper, "the tank reads lava after the fill", world.getFluidInTank(0).isSameFluid(lava()));
            assertEquals(helper, "drain of the lava", FluidHandler.BUCKET, world.drain(FluidHandler.BUCKET, transaction).amount());
            assertEquals(helper, "fill with water", FluidHandler.BUCKET, world.fill(water(), transaction));
            assertTrue(helper, "water source before the commit", helper.getLevel().getFluidState(pos).is(Fluids.WATER));
            return 0;
        });
        assertTrue(helper, "water source after drain, fill lava, drain, fill water", helper.getLevel().getFluidState(pos).is(Fluids.WATER));

        Transaction.run(transaction -> {
            world.drain(FluidHandler.BUCKET, transaction);
            return world.fill(lava(), transaction);
        });
        assertTrue(helper, "lava source after drain, fill lava", helper.getLevel().getFluidState(pos).is(Fluids.LAVA));
        helper.succeed();
    }

    public static void worldFluidHandlerCancelsFillByDrain(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.AIR);
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final FluidHandler world = requireNonNull(WorldFluidHandler.of(helper.getLevel(), pos));

        Transaction.run(transaction -> {
            assertEquals(helper, "fill with water", FluidHandler.BUCKET, world.fill(water(), transaction));
            assertEquals(helper, "second fill while full", 0, world.fill(lava(), transaction));
            assertEquals(helper, "drain of the water", FluidHandler.BUCKET, world.drain(FluidHandler.BUCKET, transaction).amount());
            assertEquals(helper, "fill with lava", FluidHandler.BUCKET, world.fill(lava(), transaction));
            assertEquals(helper, "drain of the lava", FluidHandler.BUCKET, world.drain(FluidHandler.BUCKET, transaction).amount());
            assertTrue(helper, "the tank reads empty after the drain", world.getFluidInTank(0).isEmpty());
            return 0;
        });
        assertTrue(helper, "no fluid after fill, drain, fill lava, drain", helper.getLevel().getFluidState(pos).isEmpty());

        Transaction.run(transaction -> {
            world.fill(water(), transaction);
            world.drain(FluidHandler.BUCKET, transaction);
            return world.fill(lava(), transaction);
        });
        assertTrue(helper, "lava source after fill, drain, fill lava", helper.getLevel().getFluidState(pos).is(Fluids.LAVA));
        helper.succeed();
    }

    public static void worldFluidHandlerRefillsWaterloggedBlockWithWaterOnly(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final FluidHandler world = requireNonNull(WorldFluidHandler.of(helper.getLevel(), pos));

        Transaction.run(transaction -> {
            assertEquals(helper, "drain of the waterlogged slab", FluidHandler.BUCKET, world.drain(FluidHandler.BUCKET, transaction).amount());
            assertEquals(helper, "fill with lava after the drain", 0, world.fill(lava(), transaction));
            return world.fill(water(), transaction);
        });
        assertTrue(helper, "slab is still waterlogged after drain, fill water", helper.getBlockState(DEVICE_POS).getValue(BlockStateProperties.WATERLOGGED));
        helper.succeed();
    }

    public static void fluidTransferIntoRejectingContainerKeepsFluid(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.WATER);
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final FluidHandler world = requireNonNull(WorldFluidHandler.of(helper.getLevel(), pos));
        final ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(BUCKET, 2));
        inventory.setStackInSlot(1, new ItemStack(DIRT, 64));
        final FluidHandler buckets = requireNonNull(ContainerFluidHandler.of(inventory, 0));

        assertEquals(helper, "fluid moved into buckets without room for a filled one", 0,
            Transaction.run(transaction -> FluidHandlerUtils.transfer(world, buckets, FluidHandler.BUCKET, transaction)));
        assertTrue(helper, "water source after the failed move", helper.getLevel().getFluidState(pos).isSource());
        assertEquals(helper, "empty buckets after the failed move", 2, inventory.getStackInSlot(0).getCount());
        assertEquals(helper, "dirt after the failed move", 64, inventory.getStackInSlot(1).getCount());

        final FluidTank tank = new FluidTank(FluidHandler.BUCKET);
        Transaction.run(transaction -> tank.fill(water(), transaction));
        assertEquals(helper, "fluid moved from a tank into buckets without room for a filled one", 0,
            Transaction.run(transaction -> FluidHandlerUtils.transfer(tank, buckets, FluidHandler.BUCKET, transaction)));
        assertEquals(helper, "tank after the failed move", FluidHandler.BUCKET, tank.getFluid().amount());
        helper.succeed();
    }

    public static void craftingWithRemainderThatDoesNotFitAborts(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final CraftingModuleDevice module = new CraftingModuleDevice(new ItemStack(Items.CRAFTING_MODULE.get()), robot.entity(), robot.entity());
        final ItemStackHandler inventory = robot.entity().getInventory();
        for (final int slot : new int[]{0, 1, 3, 4}) {
            inventory.setStackInSlot(slot, new ItemStack(HONEY_BOTTLE, 2));
        }
        inventory.setStackInSlot(9, new ItemStack(DIRT, 64));
        inventory.setStackInSlot(10, new ItemStack(DIRT, 64));

        assertEquals(helper, "crafted without room for the glass bottles", 0, module.craft());
        assertEquals(helper, "honey bottles after the aborted craft", 2, inventory.getStackInSlot(0).getCount());
        assertTrue(helper, "output slot after the aborted craft is empty", inventory.getStackInSlot(11).isEmpty());

        inventory.setStackInSlot(10, ItemStack.EMPTY);
        assertEquals(helper, "crafted with room for the glass bottles", 1, module.craft());
        assertEquals(helper, "honey bottles after the craft", 1, inventory.getStackInSlot(0).getCount());
        assertTrue(helper, "honey block after the craft", inventory.getStackInSlot(10).is(HONEY_BLOCK));
        assertTrue(helper, "glass bottles after the craft", inventory.getStackInSlot(11).is(GLASS_BOTTLE));
        assertEquals(helper, "glass bottles after the craft", 4, inventory.getStackInSlot(11).getCount());
        helper.succeed();
    }

    public static void diskDriveSwapEjectsOldMedium(final GameTestHelper helper) {
        final DiskDriveFixture drive = DiskDriveFixture.place(helper);
        final ItemStack first = new ItemStack(Items.FLOPPY.get());
        first.set(DataComponents.CUSTOM_NAME, Component.literal("first"));
        final ItemStack second = new ItemStack(Items.FLOPPY.get());
        second.set(DataComponents.CUSTOM_NAME, Component.literal("second"));

        drive.insert(first);
        drive.insert(second);

        assertValue(helper, "medium in the drive", second.getHoverName(), drive.floppy().getHoverName());
        assertValue(helper, "ejected medium", first.getHoverName(), Drops.single(helper).getHoverName());
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static FluidStack water() {
        return new FluidStack(Fluids.WATER, FluidHandler.BUCKET);
    }

    private static FluidStack lava() {
        return new FluidStack(Fluids.LAVA, FluidHandler.BUCKET);
    }

    private static ItemStackHandler inventory(final ItemStack stack) {
        final ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, stack);
        return inventory;
    }

    private static List<ItemEntity> itemEntities(final GameTestHelper helper, final BlockPos pos, final ItemStack stack) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos), entity -> ItemStack.isSameItem(entity.getItem(), stack));
    }

    // --------------------------------------------------------------------- //

    private TransferTests() {
    }
}
