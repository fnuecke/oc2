package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.BusCableBlock.ConnectionType;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.*;
import net.minecraft.state.EnumProperty;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class BusInterfaceItem extends ModBlockItem {
    public BusInterfaceItem() {
        super(Blocks.BUS_CABLE.get());
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final @Nullable World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.busInterfaceEnergyPerTick, tooltip);
    }

    @Override
    public ActionResultType useOn(final ItemUseContext context) {
        final Vector3d localHitPos = context.getClickLocation().subtract(Vector3d.atCenterOf(context.getClickedPos()));
        final Direction side = Direction.getNearest(localHitPos.x, localHitPos.y, localHitPos.z);
        final ActionResultType result = tryAddToBlock(context, side);
        return result.consumesAction() ? result : super.useOn(context);
    }

    @Override
    public ActionResultType place(final BlockItemUseContext context) {
        final ActionResultType result = tryAddToBlock(context, context.getClickedFace().getOpposite());
        return result.consumesAction() ? result : super.place(context);
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    public void fillItemCategory(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (allowdedIn(group)) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public void registerBlocks(final Map<Block, Item> map, final Item item) {
    }

    @Override
    public void removeFromBlockToItemMap(final Map<Block, Item> map, final Item item) {
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    protected BlockState getPlacementState(final BlockItemUseContext context) {
        final BlockState state = super.getPlacementState(context);
        final EnumProperty<ConnectionType> connectionTypeProperty =
                BusCableBlock.FACING_TO_CONNECTION_MAP.get(context.getClickedFace().getOpposite());
        return state
                .setValue(BusCableBlock.HAS_CABLE, false)
                .setValue(connectionTypeProperty, ConnectionType.INTERFACE);
    }

    ///////////////////////////////////////////////////////////////////

    private static ActionResultType tryAddToBlock(final ItemUseContext context, final Direction side) {
        final World world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = world.getBlockState(pos);

        if (BusCableBlock.addInterface(world, pos, state, side)) {
            final PlayerEntity player = context.getPlayer();
            final ItemStack stack = context.getItemInHand();

            if (player instanceof ServerPlayerEntity) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) player, pos, stack);
            }

            WorldUtils.playSound(world, pos, state.getSoundType(world, pos, player), SoundType::getPlaceSound);

            if (player == null || !player.abilities.instabuild) {
                stack.shrink(1);
            }

            return ActionResultType.sidedSuccess(world.isClientSide);
        }

        return ActionResultType.PASS;
    }
}
