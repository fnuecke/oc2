/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.MinecraftBootstrap;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import li.cil.oc2.common.bus.device.rpc.item.BlockOperationsModuleDevice;
import li.cil.oc2.common.bus.device.rpc.item.InventoryOperationsModuleDevice;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrap.class)
public class RPCDeviceDocumentationTests {
    @Test
    public void itemHandlerSlotParametersAreNamed() {
        final List<RPCMethodGroup> groups = new ObjectDevice(new ItemHandlerDevice(new EmptyItemHandler())).getMethodGroups();

        for (final String name : List.of("getItemStackInSlot", "getItemSlotLimit")) {
            for (final RPCMethod overload : overloadsOf(groups, name)) {
                for (final RPCParameter parameter : overload.getParameters()) {
                    assertEquals("slot", parameter.getName().orElse(null),
                        name + " must name its parameter, or it documents as argN");
                }
            }
        }
    }

    @Test
    public void robotModuleCallbacksAreFullyDocumented() {
        for (final AbstractItemRPCDevice device : documentedRobotModules()) {
            for (final RPCMethodGroup group : device.getMethodGroups()) {
                for (final RPCMethod overload : group.getOverloads()) {
                    assertTrue(overload.getDescription().isPresent(),
                        device.getClass().getSimpleName() + " callback \"" + group.getName() + "\" is undocumented");
                }
            }
        }
    }

    @Test
    public void blockOperationsCallbacksCarryDocumentation() {
        final List<RPCMethodGroup> groups =
            new BlockOperationsModuleDevice(ItemStack.EMPTY, null, null).getMethodGroups();

        for (final String name : List.of("excavate", "place", "durability")) {
            for (final RPCMethod overload : overloadsOf(groups, name)) {
                assertTrue(overload.getDescription().isPresent(), name + " has no description");
                assertTrue(overload.getReturnValueDescription().isPresent(), name + " has no return description");
                for (final RPCParameter parameter : overload.getParameters()) {
                    assertTrue(parameter.getDescription().isPresent(),
                        name + " parameter " + parameter.getName().orElse("?") + " has no description");
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

    private static List<AbstractItemRPCDevice> documentedRobotModules() {
        return List.of(
            new BlockOperationsModuleDevice(ItemStack.EMPTY, null, null),
            new InventoryOperationsModuleDevice(ItemStack.EMPTY, null, null));
    }

    private static Set<RPCMethod> overloadsOf(final List<RPCMethodGroup> groups, final String name) {
        return groups.stream()
            .filter(group -> group.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no callback named " + name))
            .getOverloads();
    }

    private static final class EmptyItemHandler implements ItemHandler {
        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return ItemStack.EMPTY;
        }
    }
}
