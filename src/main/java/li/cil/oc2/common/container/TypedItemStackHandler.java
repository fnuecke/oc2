package li.cil.oc2.common.container;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;
import org.jetbrains.annotations.NotNull;

public class TypedItemStackHandler extends FixedSizeItemStackHandler {
    private final ITag<Item> deviceType;

    ///////////////////////////////////////////////////////////////////

    public TypedItemStackHandler(final int size, final ITag<Item> deviceType) {
        super(size);
        this.deviceType = deviceType;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isItemValid(final int slot, @NotNull final ItemStack stack) {
        return super.isItemValid(slot, stack) && !stack.isEmpty() && deviceType.contains(stack.getItem());
    }
}
