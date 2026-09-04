/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.bus.device.rpc.item.BlockOperationsModuleDevice;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RPCDeviceDocumentationTests {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

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
    public void blockOperationsDocumentsOnlyCallbacksThatExist() {
        final BlockOperationsModuleDevice device = new BlockOperationsModuleDevice(ItemStack.EMPTY, null, null);
        final Set<String> callbacks = device.getMethodGroups().stream()
            .map(RPCMethodGroup::getName).collect(Collectors.toSet());

        final RecordingVisitor visitor = new RecordingVisitor();
        device.getDeviceDocumentation(visitor);

        assertFalse(visitor.documented.isEmpty(), "no documentation was declared at all");
        for (final String documented : visitor.documented) {
            assertTrue(callbacks.contains(documented),
                "documented callback \"" + documented + "\" does not exist; its documentation is silently dropped");
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

    private static Set<RPCMethod> overloadsOf(final List<RPCMethodGroup> groups, final String name) {
        return groups.stream()
            .filter(group -> group.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no callback named " + name))
            .getOverloads();
    }

    private static final class RecordingVisitor implements DocumentedDevice.DeviceVisitor,
        DocumentedDevice.CallbackVisitor {
        private final Set<String> documented = new LinkedHashSet<>();

        @Override
        public DocumentedDevice.CallbackVisitor visitCallback(final String callbackName) {
            documented.add(callbackName);
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor description(final String value) {
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor returnValueDescription(final String value) {
            return this;
        }

        @Override
        public DocumentedDevice.CallbackVisitor parameterDescription(final String parameterName, final String value) {
            return this;
        }
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
