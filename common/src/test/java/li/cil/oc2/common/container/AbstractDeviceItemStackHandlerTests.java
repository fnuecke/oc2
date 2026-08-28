/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.bus.AbstractItemDeviceBusElement;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public final class AbstractDeviceItemStackHandlerTests {
    private AbstractItemDeviceBusElement busElement;
    private AbstractDeviceItemStackHandler handler;

    @BeforeAll
    public static void setupAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    public void setupEach() {
        busElement = mock(AbstractItemDeviceBusElement.class);
        handler = new AbstractDeviceItemStackHandler(1) {
            @Override
            public AbstractItemDeviceBusElement getBusElement() {
                return busElement;
            }
        };
    }

    @Test
    public void readExportsDeviceDataIntoStack() {
        final ItemStack stack = new ItemStack(Items.DIAMOND);
        handler.setStackInSlot(0, stack);
        clearInvocations(busElement);

        handler.getStackInSlot(0);

        verify(busElement).exportDeviceDataToItemStack(0, stack);
    }

    @Test
    public void slotChangeDoesNotExportIntoIncomingStack() {
        handler.setStackInSlot(0, new ItemStack(Items.DIAMOND));
        clearInvocations(busElement);

        final ItemStack incoming = new ItemStack(Items.EMERALD);
        handler.setStackInSlot(0, incoming);

        verify(busElement, never()).exportDeviceDataToItemStack(anyInt(), any());
        verify(busElement).handleSlotContentsChanged(0, incoming);
    }
}
