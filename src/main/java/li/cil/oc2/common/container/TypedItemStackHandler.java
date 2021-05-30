package li.cil.oc2.common.container;

import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TypedContainerHelper extends FixedSizeContainerHelper {
    private final Tag<Item> deviceType;

    ///////////////////////////////////////////////////////////////////

    public TypedContainerHelper(final int size, final Tag<Item> deviceType) {
        super(size);
        this.deviceType = deviceType;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && !stack.isEmpty() && deviceType.contains(stack.getItem());
    }
}
