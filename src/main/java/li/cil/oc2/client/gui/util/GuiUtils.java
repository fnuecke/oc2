package li.cil.oc2.client.gui.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.container.TypedSlotItemHandler;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Optional;

public final class GuiUtils {
    public static final ResourceLocation WARN_ICON = new ResourceLocation(API.MOD_ID, "textures/gui/overlay/slot_warn.png");
    public static final ResourceLocation INFO_ICON = new ResourceLocation(API.MOD_ID, "textures/gui/overlay/slot_info.png");

    private static final int SLOT_SIZE = 18;
    private static final int DEVICE_INFO_ICON_SIZE = 28;
    private static final int RELATIVE_ICON_POSITION = (SLOT_SIZE - DEVICE_INFO_ICON_SIZE) / 2;

    ///////////////////////////////////////////////////////////////////

    public static <TContainer extends Container> void renderMissingDeviceInfoIcon(final MatrixStack matrixStack, final ContainerScreen<TContainer> screen, final DeviceType type, final ResourceLocation icon) {
        findFirstSlotOfTypeIfAllSlotsOfTypeEmpty(screen.getMenu(), type).ifPresent(slot -> {
            screen.getMinecraft().getTextureManager().bind(icon);
            AbstractGui.blit(matrixStack,
                    screen.getGuiLeft() + slot.x - 1 + RELATIVE_ICON_POSITION,
                    screen.getGuiTop() + slot.y - 1 + RELATIVE_ICON_POSITION,
                    200,
                    0,
                    0,
                    DEVICE_INFO_ICON_SIZE,
                    DEVICE_INFO_ICON_SIZE,
                    DEVICE_INFO_ICON_SIZE,
                    DEVICE_INFO_ICON_SIZE);
        });
    }

    public static <TContainer extends Container> void renderMissingDeviceInfoTooltip(final MatrixStack matrixStack, final ContainerScreen<TContainer> screen, final int mouseX, final int mouseY, final DeviceType type, final ITextComponent tooltip) {
        final boolean isCursorHoldingStack = !screen.getMinecraft().player.inventory.items.isEmpty();
        if (isCursorHoldingStack) {
            return;
        }

        final Slot hoveredSlot = screen.getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            return;
        }

        findFirstSlotOfTypeIfAllSlotsOfTypeEmpty(screen.getMenu(), type).ifPresent(slot -> {
            if (slot == hoveredSlot) {
                screen.renderTooltip(matrixStack, tooltip, mouseX, mouseY);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    private static Optional<TypedSlotItemHandler> findFirstSlotOfTypeIfAllSlotsOfTypeEmpty(final Container container, final DeviceType type) {
        TypedSlotItemHandler firstSlot = null;
        for (final Slot slot : container.slots) {
            if (slot instanceof TypedSlotItemHandler) {
                final TypedSlotItemHandler typedSlot = (TypedSlotItemHandler) slot;
                final DeviceType slotType = typedSlot.getDeviceType();
                if (slotType == type) {
                    if (slot.hasItem()) {
                        return Optional.empty();
                    } else if (firstSlot == null) {
                        firstSlot = typedSlot;
                    }
                }
            }
        }
        return Optional.ofNullable(firstSlot);
    }
}
