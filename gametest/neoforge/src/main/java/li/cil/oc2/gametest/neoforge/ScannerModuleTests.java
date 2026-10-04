/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.item.ScannerModuleDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.device.GuestTestDevices;
import li.cil.oc2.gametest.fixture.GuestTests;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static li.cil.oc2.gametest.util.DeviceCalls.*;
import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScannerModuleTests {
    private static final String GUEST_BATCH = "oc2_scanner_guest";
    private static final String GUEST_SUITE = "robot_scanner";
    private static final int SETTLE_TICKS = 20;
    private static final int COOLDOWN_TICKS = 100;

    private static final int INSPECT_CODE = 1;
    private static final int SCAN_CODE = 2;
    private static final int GET_SCAN_LAYER_CODE = 3;
    private static final int CAN_SEE_SKY_CODE = 4;
    private static final int SYSTEM_GET_BLOCK_NAME_CODE = 5;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void robotWithModuleProvidesTheScanner(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> robot.install(DeviceTypes.ROBOT_MODULE.get(), new ItemStack(Items.SCANNER_MODULE.get())))
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                for (final Device device : robot.devices()) {
                    if (device instanceof final RPCDevice rpc && rpc.getTypeNames().contains("scanner")) {
                        return;
                    }
                }
                throw failure(helper, "no scanner device on the robot's bus");
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void inspectIdentifiesBlockFluidAndEntities(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final ScannerModuleDevice scanner = scannerFor(robot);

                robot.putBlockInFront(Blocks.AIR);
                ScannerModuleDevice.Inspection inspection = scanner.inspect(RobotOperationSide.FRONT);
                assertTrue(helper, "air has no block", inspection.block() == null);
                assertTrue(helper, "air has no fluid", inspection.fluid() == null);
                assertEquals(helper, "nothing is in front", 0, inspection.entities().length);

                helper.getLevel().setBlockAndUpdate(robot.frontPos(),
                    Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
                inspection = scanner.inspect(RobotOperationSide.FRONT);
                assertTrue(helper, "a waterlogged chain is a chain, got " + inspection.block(), "minecraft:chain".equals(inspection.block()));
                assertTrue(helper, "and holds water, got " + inspection.fluid(), "minecraft:water".equals(inspection.fluid()));

                helper.getLevel().setBlockAndUpdate(robot.frontPos(),
                    Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 3));
                inspection = scanner.inspect(RobotOperationSide.FRONT);
                assertTrue(helper, "flowing water reads as water, got " + inspection.fluid(), "minecraft:water".equals(inspection.fluid()));

                robot.putBlockInFront(Blocks.AIR);
                spawnCrystal(helper, Vec3.atCenterOf(robot.frontPos()));
                inspection = scanner.inspect(RobotOperationSide.FRONT);
                assertTrue(helper, "the crystal is listed by type, got " + Arrays.toString(inspection.entities()),
                    Arrays.equals(new String[]{"minecraft:end_crystal"}, inspection.entities()));
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void scanReportsHardnessInCompassOrder(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final BlockPos center = robot.blockPos();
                put(helper, center, 1, 0, 0, Blocks.STONE.defaultBlockState());
                put(helper, center, 0, 0, -2, Blocks.DIRT.defaultBlockState());
                put(helper, center, -1, 0, 1, Blocks.GLASS.defaultBlockState());
                put(helper, center, 0, 0, 2, Blocks.OBSIDIAN.defaultBlockState());
                put(helper, center, -2, 0, 0, Blocks.STRUCTURE_VOID.defaultBlockState());
                put(helper, center, -3, -1, 2, Blocks.BEDROCK.defaultBlockState());
                put(helper, center, 2, -1, 0, Blocks.WATER.defaultBlockState());
                put(helper, center, -1, -1, -1, Blocks.BUBBLE_COLUMN.defaultBlockState());

                final byte[] hardness = scannerFor(robot).scan().hardness();
                assertEquals(helper, "the scan covers seven cubed blocks", 343, hardness.length);
                assertHardness(helper, hardness, 0, 0, 0, 0, "the robot's own space is air");
                assertHardness(helper, hardness, 1, 0, 0, 15, "stone to the east");
                assertHardness(helper, hardness, 0, 0, -2, 5, "dirt to the north");
                assertHardness(helper, hardness, -1, 0, 1, 3, "glass to the south-west");
                assertHardness(helper, hardness, 0, 0, 2, 253, "obsidian clamps");
                assertHardness(helper, hardness, -2, 0, 0, 1, "a block without hardness is not air");
                assertHardness(helper, hardness, -3, -1, 2, 255, "bedrock is unbreakable");
                assertHardness(helper, hardness, 2, -1, 0, 254, "water is a fluid");
                assertHardness(helper, hardness, -1, -1, -1, 254, "a bubble column is a fluid");
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void scanListsTheClosestEntitiesFirstAndCapsThem(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final Vec3 center = Vec3.atCenterOf(robot.blockPos());
                spawnCrystal(helper, center.add(1, 0, 0));
                for (int i = 0; i < 25; i++) {
                    final ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
                    stand.setNoGravity(true);
                    stand.setPos(center.add(-2.5, 0, i % 5 * 0.2 - 0.4));
                    helper.getLevel().addFreshEntity(stand);
                }

                final String[] entities = scannerFor(robot).scan().entities();
                assertEquals(helper, "entities are capped", 20, entities.length);
                assertTrue(helper, "the closest comes first, got " + entities[0], "minecraft:end_crystal".equals(entities[0]));
                assertTrue(helper, "the rest are armor stands", Arrays.stream(entities).skip(1).allMatch("minecraft:armor_stand"::equals));
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void scanRechargesAcrossSaving(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final ScannerModuleDevice scanner = scannerFor(robot);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                scanner.scan();
                assertRecharging(helper, scanner, "a second scan right away");

                final ScannerModuleDevice reloaded = scannerFor(robot);
                reloaded.deserializeNBT(scanner.serializeNBT());
                assertRecharging(helper, reloaded, "a reloaded scanner");
            })
            .thenExecuteAfter(COOLDOWN_TICKS, scanner::scan)
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void skyVisibilityFollowsTheBlocksAbove(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final ScannerModuleDevice scanner = scannerFor(robot);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                assertTrue(helper, "the test area is open to the sky", scanner.canSeeSky());
                helper.getLevel().setBlockAndUpdate(robot.blockPos().above(2), Blocks.STONE.defaultBlockState());
            })
            .thenExecuteAfter(5, () -> assertTrue(helper, "a roof hides the sky", !scanner.canSeeSky()))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void ioReadsInspectionsAndScanLayers(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final ScannerModuleDevice scanner = scannerFor(robot);

                helper.getLevel().setBlockAndUpdate(robot.frontPos(),
                    Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
                final byte[] inspection = invokeIo(scanner, INSPECT_CODE, SIDE_FRONT);
                assertEquals(helper, "an inspection is five bytes", 5, inspection.length);
                assertEquals(helper, "the block id", BuiltInRegistries.BLOCK.getId(Blocks.CHAIN), u16(inspection, 0));
                assertEquals(helper, "the fluid id", BuiltInRegistries.FLUID.getId(Fluids.WATER), u16(inspection, 2));
                assertEquals(helper, "the entity count", 0, inspection[4]);

                final byte[] hardness = scanner.scan().hardness();
                for (int layer = 0; layer < 7; layer++) {
                    final byte[] expected = Arrays.copyOfRange(hardness, layer * 49, (layer + 1) * 49);
                    assertTrue(helper, "layer " + layer + " is a slice of the scan",
                        Arrays.equals(expected, invokeIo(scanner, GET_SCAN_LAYER_CODE, layer)));
                }
                assertThrows(helper, "a layer past the cube", () -> invokeIo(scanner, GET_SCAN_LAYER_CODE, 7));

                assertTrue(helper, "SYSTEM names the inspected block", "minecraft:chain".equals(new String(
                    invokeIo(robot.ioDevice("SYSTEM"), SYSTEM_GET_BLOCK_NAME_CODE, inspection[0] & 0xFF, inspection[1] & 0xFF), US_ASCII)));

                assertEquals(helper, "the sky reads as one byte", scanner.canSeeSky() ? 1 : 0, invokeIo(scanner, CAN_SEE_SKY_CODE)[0]);

                final ScannerModuleDevice other = scannerFor(robot);
                assertEquals(helper, "an IO scan reports the entity count", 1, invokeIo(other, SCAN_CODE).length);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = GUEST_BATCH)
    public static void guestReadsScanResults(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final GuestTests tests = robot.guestTests();

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                robot.charge();
                Hardware.installLinuxWithExtraMemory(robot);
                robot.install(DeviceTypes.ROBOT_MODULE.get(), new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get()))
                    .install(DeviceTypes.ROBOT_MODULE.get(), new ItemStack(Items.SCANNER_MODULE.get()));
            })
            .thenExecuteAfter(SETTLE_TICKS, robot::start)
            .thenWaitUntil(() -> {
                robot.keepAlive();
                tests.requireReady();
            })
            .thenExecute(() -> tests.run(GUEST_SUITE))
            .thenWaitUntil(() -> {
                robot.keepAlive();
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ScannerModuleDevice scannerFor(final RobotFixture robot) {
        return new ScannerModuleDevice(new ItemStack(Items.SCANNER_MODULE.get()), robot.entity());
    }

    private static void put(final GameTestHelper helper, final BlockPos center, final int x, final int y, final int z, final BlockState state) {
        helper.getLevel().setBlockAndUpdate(center.offset(x, y, z), state);
    }

    private static void spawnCrystal(final GameTestHelper helper, final Vec3 position) {
        final EndCrystal crystal = new EndCrystal(helper.getLevel(), position.x, position.y - 0.5, position.z);
        crystal.setShowBottom(false);
        helper.getLevel().addFreshEntity(crystal);
    }

    private static void assertHardness(final GameTestHelper helper, final byte[] hardness, final int x, final int y, final int z,
                                       final int expected, final String what) {
        assertEquals(helper, what, expected, hardness[(y + 3) * 49 + (z + 3) * 7 + x + 3] & 0xFF);
    }

    private static void assertRecharging(final GameTestHelper helper, final ScannerModuleDevice scanner, final String what) {
        try {
            scanner.scan();
        } catch (final IllegalStateException e) {
            return;
        }
        throw failure(helper, what + " should fail while recharging");
    }

    // --------------------------------------------------------------------- //

    private ScannerModuleTests() {
    }
}
