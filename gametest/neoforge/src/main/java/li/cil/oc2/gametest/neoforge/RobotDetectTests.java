/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

import static li.cil.oc2.gametest.BlockOperationsModuleTests.ROBOT_POS;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotDetectTests {
    @GameTest(template = TEMPLATE)
    public static void solidBlocksReadAsSolid(final GameTestHelper helper) {
        requireDetects(helper, Blocks.STONE.defaultBlockState(), "solid");
    }

    @GameTest(template = TEMPLATE)
    public static void emptySpaceReadsAsAir(final GameTestHelper helper) {
        requireDetects(helper, Blocks.AIR.defaultBlockState(), "air");
    }

    @GameTest(template = TEMPLATE)
    public static void fluidsReadAsFluid(final GameTestHelper helper) {
        requireDetects(helper, Blocks.WATER.defaultBlockState(), "fluid");
    }

    @GameTest(template = TEMPLATE)
    public static void blocksTheRobotCanMoveThroughReadAsAir(final GameTestHelper helper) {
        requireDetects(helper, Blocks.SHORT_GRASS.defaultBlockState(), "air");
    }

    @GameTest(template = TEMPLATE)
    public static void obstructionsWinOverFluid(final GameTestHelper helper) {
        requireDetects(helper, Blocks.CHAIN.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true), "solid");
    }

    // --------------------------------------------------------------------- //

    private static void requireDetects(final GameTestHelper helper, final BlockState state, final String expected) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(60, () -> {
                final BlockPos target = robot.frontPos();
                helper.getLevel().setBlockAndUpdate(target, state);

                final String actual = String.valueOf(invokeDetect(robot, "front"));
                if (!expected.equals(actual)) {
                    throw new GameTestAssertException("detect() reported \"" + actual + "\" for "
                        + helper.getLevel().getBlockState(target) + ", expected \"" + expected + "\"");
                }
            })
            .thenSucceed();
    }

    private static Object invokeDetect(final RobotFixture robot, final String side) {
        final Gson gson = RPCTypeAdapters.beginBuildGson().create();
        final JsonArray parameters = new JsonArray();
        parameters.add(side);

        final RPCInvocation invocation = new RPCInvocation() {
            @Override
            public JsonArray getParameters() {
                return parameters;
            }

            @Override
            public Gson getGson() {
                return gson;
            }

            @Override
            public Optional<Object[]> tryDeserializeParameters(final RPCParameter... parameterTypes) {
                if (parameterTypes.length != parameters.size()) {
                    return Optional.empty();
                }

                final Object[] result = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    try {
                        result[i] = gson.fromJson(parameters.get(i), parameterTypes[i].getType());
                    } catch (final Throwable e) {
                        return Optional.empty();
                    }
                }
                return Optional.of(result);
            }
        };

        for (final Device device : ((AbstractVirtualMachine) robot.entity().getVirtualMachine()).getBusController().getDevices()) {
            if (!(device instanceof final RPCDevice rpcDevice)) {
                continue;
            }
            for (final RPCMethodGroup group : rpcDevice.getMethodGroups()) {
                if (!"detect".equals(group.getName())) {
                    continue;
                }
                final RPCMethod method = group.findOverload(invocation)
                    .orElseThrow(() -> new GameTestAssertException("no detect overload matched a side argument"));
                try {
                    return method.invoke(invocation);
                } catch (final Throwable e) {
                    throw new GameTestAssertException("detect threw: " + e);
                }
            }
        }

        throw new GameTestAssertException("the robot exposes no detect callback");
    }

    private RobotDetectTests() {
    }
}
