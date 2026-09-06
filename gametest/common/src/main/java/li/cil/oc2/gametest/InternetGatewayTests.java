/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.EnergyStorage;
import li.cil.oc2.common.inet.InternetManager;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import static li.cil.oc2.gametest.TestSupport.*;

public final class InternetGatewayTests {
    private static final BlockPos GATEWAY_POS = new BlockPos(2, WORK_Y, 2);

    // --------------------------------------------------------------------- //

    public static void gatewayPlacesAndOffersItsCapabilities(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.INTERNET_GATEWAY.get()), GATEWAY_POS);

        final BlockEntity blockEntity = helper.getBlockEntity(GATEWAY_POS);
        assertNotNull(helper, blockEntity, "gateway block entity");
        assertTrue(helper, "the block entity should be an internet gateway",
            blockEntity instanceof InternetGatewayBlockEntity);

        final NetworkInterface networkInterface =
            Capabilities.get(blockEntity, Capabilities.NETWORK_INTERFACE, Direction.NORTH);
        assertNotNull(helper, networkInterface, "network interface capability");

        final EnergyStorage energy =
            Capabilities.get(blockEntity, Capabilities.ENERGY_STORAGE, Direction.NORTH);
        assertNotNull(helper, energy, "energy storage capability");

        helper.succeed();
    }

    public static void gatewayWithoutAConnectionDropsFrames(final GameTestHelper helper) {
        final boolean wasEnabled = withInternetAccess(false);
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.INTERNET_GATEWAY.get()), GATEWAY_POS);

        final BlockEntity blockEntity = helper.getBlockEntity(GATEWAY_POS);
        assertNotNull(helper, blockEntity, "gateway block entity");
        final NetworkInterface networkInterface =
            Capabilities.get(blockEntity, Capabilities.NETWORK_INTERFACE, Direction.NORTH);
        assertNotNull(helper, networkInterface, "network interface capability");

        final EnergyStorage energy =
            Capabilities.get(blockEntity, Capabilities.ENERGY_STORAGE, Direction.NORTH);
        assertNotNull(helper, energy, "energy storage capability");
        energy.receiveEnergy(Config.internetGatewayEnergyStorage, false);

        final byte[] frame = new byte[64];
        for (int i = 0; i < 1000; ++i) {
            networkInterface.writeEthernetFrame(networkInterface, frame, 8);
        }

        final boolean handedBack = networkInterface.readEthernetFrame() != null;
        final boolean queued = ((InternetGatewayBlockEntity) blockEntity).readInternetFrame() != null;
        restoreInternetAccess(wasEnabled);

        assertTrue(helper, "a gateway with nowhere to send should not hand anything back", !handedBack);
        assertTrue(helper, "frames must not be queued toward an internet stack that is not there", !queued);

        helper.succeed();
    }

    public static void gatewayKeepsItsEnergyAcrossAReload(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.INTERNET_GATEWAY.get()), GATEWAY_POS);

        final BlockPos absolute = helper.absolutePos(GATEWAY_POS);
        final BlockEntity blockEntity = helper.getBlockEntity(GATEWAY_POS);
        assertNotNull(helper, blockEntity, "gateway block entity");

        final EnergyStorage energy =
            Capabilities.get(blockEntity, Capabilities.ENERGY_STORAGE, Direction.NORTH);
        assertNotNull(helper, energy, "energy storage capability");

        final long received = energy.receiveEnergy(500, false);
        assertTrue(helper, "the gateway should accept energy", received > 0);

        final var registries = helper.getLevel().registryAccess();
        final var tag = blockEntity.saveWithoutMetadata(registries);

        final var reloaded = BlockEntities.INTERNET_GATEWAY.get()
            .create(absolute, helper.getBlockState(GATEWAY_POS));
        assertNotNull(helper, reloaded, "reloaded gateway block entity");
        reloaded.loadWithComponents(tag, registries);

        final EnergyStorage reloadedEnergy =
            reloaded.getCapability(Capabilities.ENERGY_STORAGE, Direction.NORTH);
        assertNotNull(helper, reloadedEnergy, "reloaded energy storage");
        assertEquals(helper, "stored energy should survive a save and load",
            received, reloadedEnergy.getEnergyStored());

        helper.succeed();
    }

    public static void gatewayWithoutInternetAccessIsNotOperational(final GameTestHelper helper) {
        final boolean wasEnabled = withInternetAccess(false);
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.INTERNET_GATEWAY.get()), GATEWAY_POS);

        final BlockEntity blockEntity = helper.getBlockEntity(GATEWAY_POS);
        assertNotNull(helper, blockEntity, "gateway block entity");
        final InternetGatewayBlockEntity gateway = (InternetGatewayBlockEntity) blockEntity;

        final EnergyStorage energy =
            Capabilities.get(blockEntity, Capabilities.ENERGY_STORAGE, Direction.NORTH);
        assertNotNull(helper, energy, "energy storage capability");
        energy.receiveEnergy(Config.internetGatewayEnergyStorage, false);

        helper.startSequence()
            .thenExecuteAfter(2, () -> {
                final boolean isOperational = gateway.isOperational();
                restoreInternetAccess(wasEnabled);
                assertTrue(helper,
                    "a powered gateway with no internet access must not show as operational",
                    !isOperational);
            })
            .thenSucceed();
    }

    public static void gatewayTracksItsOperationalStateFromEnergy(final GameTestHelper helper) {
        final boolean wasEnabled = withInternetAccess(true);
        InternetManager.start();

        final InternetGatewayBlockEntity gateway;
        final EnergyStorage energy;
        try {
            final Player player = fakePlayer(helper);
            place(helper, player, new ItemStack(Items.INTERNET_GATEWAY.get()), GATEWAY_POS);

            final BlockEntity blockEntity = helper.getBlockEntity(GATEWAY_POS);
            assertNotNull(helper, blockEntity, "gateway block entity");
            gateway = (InternetGatewayBlockEntity) blockEntity;

            energy = Capabilities.get(blockEntity, Capabilities.ENERGY_STORAGE, Direction.NORTH);
            assertNotNull(helper, energy, "energy storage capability");

            assertTrue(helper, "a gateway without energy should not show as operational",
                !gateway.isOperational());
        } catch (final Throwable t) {
            restoreInternetAccess(wasEnabled);
            throw t;
        }

        helper.startSequence()
            .thenExecute(() -> energy.receiveEnergy(Config.internetGatewayEnergyPerPacket, false))
            .thenExecuteAfter(2, () -> {
                // Read first, then put the server back, so a failed assertion cannot leak the
                // manager into the tests that run after this one.
                final boolean isOperational = gateway.isOperational();
                restoreInternetAccess(wasEnabled);
                assertTrue(helper, "a powered gateway with internet access should show as operational",
                    isOperational);
            })
            .thenSucceed();
    }

    /**
     * @return the previous setting, to hand back to {@link #restoreInternetAccess}
     */
    private static boolean withInternetAccess(final boolean enabled) {
        final boolean wasEnabled = Config.internetEnabled;
        Config.internetEnabled = enabled;
        if (!enabled) {
            InternetManager.stop();
        }
        return wasEnabled;
    }

    private static void restoreInternetAccess(final boolean wasEnabled) {
        InternetManager.stop();
        Config.internetEnabled = wasEnabled;
        if (wasEnabled) {
            InternetManager.start();
        }
    }

    public static void gatewayReleasesItsCapabilitiesWhenBroken(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.INTERNET_GATEWAY.get()), GATEWAY_POS);

        breakBlock(helper, GATEWAY_POS);

        assertTrue(helper, "the block should be gone", helper.getBlockState(GATEWAY_POS).isAir());
        // Straight to the level: the helper's accessor throws for a missing block entity rather
        // than reporting one.
        assertTrue(helper, "no block entity should remain",
            helper.getLevel().getBlockEntity(helper.absolutePos(GATEWAY_POS)) == null);

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private InternetGatewayTests() {
    }
}
