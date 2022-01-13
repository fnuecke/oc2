package li.cil.oc2.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LockedSlot extends Slot {
    public LockedSlot(final Container container, final int slot, final int x, final int y) {
        super(container, slot, x, y);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean mayPlace(final ItemStack p_40231_) {
        return false;
    }

    @Override
    public boolean mayPickup(final Player p_40228_) {
        return false;
    }
}
