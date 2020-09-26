package li.cil.oc2.common.item;

import li.cil.oc2.client.gui.RISCVTestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class RISCVTesterItem extends Item {
    public RISCVTesterItem() {
        super(new Properties().group(ItemGroup.MISC).maxStackSize(1));
    }

    @Override
    public void onUse(final World world, final LivingEntity entity, final ItemStack stack, final int count) {
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final PlayerEntity player, final Hand hand) {
        if (world.isRemote()) {
            Minecraft.getInstance().displayGuiScreen(new RISCVTestScreen());
        }

        return ActionResult.resultConsume(player.getHeldItem(hand));
    }

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        if (context.getWorld().isRemote()) {
            Minecraft.getInstance().displayGuiScreen(new RISCVTestScreen());
        }

        return ActionResultType.CONSUME;
    }
}
