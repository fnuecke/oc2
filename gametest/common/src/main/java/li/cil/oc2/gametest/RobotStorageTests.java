/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static li.cil.oc2.gametest.util.TestSupport.ROBOT_POS;
import static li.cil.oc2.gametest.util.TestSupport.assertTrue;

public final class RobotStorageTests {
    public static void destroyedRobotReleasesItsBlobs(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                robot.charge();
                Hardware.installLinux(robot);
            })
            .thenExecuteAfter(20, robot::start)
            .thenWaitUntil(() -> {
                robot.charge();
                robot.assertRunState(VMRunState.RUNNING, "precondition");
            })
            .thenExecuteAfter(20, () -> {
                final CompoundTag saved = new CompoundTag();
                robot.entity().save(saved);
                final List<UUID> handles = new ArrayList<>();
                collectBlobHandles(saved, handles);
                assertTrue(helper, "memory, flash and drive blobs are referenced: " + handles, handles.size() == 3);

                robot.entity().dropSelf();

                final List<UUID> open = handles.stream().filter(BlobStorage::isOpen).toList();
                final List<UUID> kept = handles.stream().filter(BlobStorage::exists).toList();
                assertTrue(helper, "destroyed robot still holds " + open, open.isEmpty());
                assertTrue(helper, "only memory is deleted, kept " + kept + " of " + handles, kept.size() == 2);
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void collectBlobHandles(final Tag tag, final List<UUID> handles) {
        if (tag instanceof final CompoundTag compound) {
            for (final String key : compound.getAllKeys()) {
                if ("blob".equals(key) && compound.hasUUID(key)) {
                    final UUID handle = compound.getUUID(key);
                    if (!handles.contains(handle)) {
                        handles.add(handle);
                    }
                } else {
                    collectBlobHandles(compound.get(key), handles);
                }
            }
        } else if (tag instanceof final ListTag list) {
            list.forEach(entry -> collectBlobHandles(entry, handles));
        }
    }

    // --------------------------------------------------------------------- //

    private RobotStorageTests() {
    }
}
