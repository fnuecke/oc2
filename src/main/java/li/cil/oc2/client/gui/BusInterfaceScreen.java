package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class BusInterfaceScreen extends Screen {
    private static final int TEXT_LEFT = 9;
    private static final int TEXT_TOP = 11;
    private static final int CONFIRM_LEFT = 206;
    private static final int CONFIRM_TOP = 9;
    private static final int CANCEL_LEFT = 219;
    private static final int CANCEL_TOP = 9;

    private final BusCableTileEntity tileEntity;
    private final Direction side;

    private EditBox nameField;

    private int left, top;

    ///////////////////////////////////////////////////////////////////

    public BusInterfaceScreen(final BusCableTileEntity tileEntity, final Direction side) {
        super(Items.BUS_INTERFACE.get().getDescription());
        this.tileEntity = tileEntity;
        this.side = side;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        getMinecraft().keyboardHandler.setSendRepeatsToGui(true);

        left = (width - Sprites.BUS_INTERFACE_SCREEN.width) / 2;
        top = (height - Sprites.BUS_INTERFACE_SCREEN.height) / 2;

        nameField = new EditBox(font, left + TEXT_LEFT, top + TEXT_TOP, 192, 12, text("{mod}.gui.bus_interface_name"));
        nameField.setCanLoseFocus(false);
        nameField.setTextColor(0xFFFFFFFF);
        nameField.setBordered(false);
        nameField.setMaxLength(32);
        nameField.setValue(tileEntity.getInterfaceName(side));
        addWidget(nameField);
        setInitialFocus(nameField);

        addRenderableWidget(new ImageButton(
                this,
                left + CONFIRM_LEFT, top + CONFIRM_TOP,
                Sprites.CONFIRM_BASE.width, Sprites.CONFIRM_BASE.height,
                new TranslatableComponent(Constants.TOOLTIP_CONFIRM),
                null,
                Sprites.CONFIRM_BASE,
                Sprites.CONFIRM_PRESSED
        ) {
            @Override
            public void onPress() {
                super.onPress();
                setInterfaceName(nameField.getValue());
                onClose();
            }
        });
        addRenderableWidget(new ImageButton(
                this,
                left + CANCEL_LEFT, top + CANCEL_TOP,
                Sprites.CANCEL_BASE.width, Sprites.CANCEL_BASE.height,
                new TranslatableComponent(Constants.TOOLTIP_CANCEL),
                null,
                Sprites.CANCEL_BASE,
                Sprites.CANCEL_PRESSED
        ) {
            @Override
            public void onPress() {
                super.onPress();
                onClose();
            }
        });
    }

    @Override
    public void onClose() {
        super.onClose();

        getMinecraft().keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void tick() {
        super.tick();
        nameField.tick();

        final Vec3 busCableCenter = Vec3.atCenterOf(tileEntity.getBlockPos());
        if (getMinecraft().player.distanceToSqr(busCableCenter) > 8 * 8) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER ||
            keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            setInterfaceName(nameField.getValue());
            onClose();
            return true;
        }

        return nameField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(final PoseStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);
        Sprites.BUS_INTERFACE_SCREEN.draw(matrixStack, left, top);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        RenderSystem.disableBlend();
        nameField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    private void setInterfaceName(final String name) {
        Network.INSTANCE.sendToServer(new BusInterfaceNameMessage.ToServer(tileEntity, side, name));
    }
}
