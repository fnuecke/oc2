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
    public void addInformation(final ItemStack stack, final @Nullable World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.busInterfaceEnergyPerTick, tooltip);
    }

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final Vector3d localHitPos = context.getHitVec().subtract(Vector3d.copyCentered(context.getPos()));
        final Direction side = Direction.getFacingFromVector(localHitPos.x, localHitPos.y, localHitPos.z);
        final ActionResultType result = tryAddToBlock(context, side);
        return result.isSuccessOrConsume() ? result : super.onItemUse(context);
    }

    @Override
    public ActionResultType tryPlace(final BlockItemUseContext context) {
        final ActionResultType result = tryAddToBlock(context, context.getFace().getOpposite());
        return result.isSuccessOrConsume() ? result : super.tryPlace(context);
    }

    @Override
    public String getTranslationKey() {
        return getDefaultTranslationKey();
    }

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (isInGroup(group)) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public void addToBlockToItemMap(final Map<Block, Item> map, final Item item) {
    }

    @Override
    public void removeFromBlockToItemMap(final Map<Block, Item> map, final Item item) {
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    protected BlockState getStateForPlacement(final BlockItemUseContext context) {
        final BlockState state = super.getStateForPlacement(context);
        final EnumProperty<ConnectionType> connectionTypeProperty =
                BusCableBlock.FACING_TO_CONNECTION_MAP.get(context.getFace().getOpposite());
        return state
                .with(BusCableBlock.HAS_CABLE, false)
                .with(connectionTypeProperty, ConnectionType.INTERFACE);
    }

    ///////////////////////////////////////////////////////////////////

    private ActionResultType tryAddToBlock(final ItemUseContext context, final Direction side) {
        final BusCableBlock busCableBlock = Blocks.BUS_CABLE.get();

        final World world = context.getWorld();
        final BlockPos pos = context.getPos();
        final BlockState state = world.getBlockState(pos);

        if (state.getBlock() == busCableBlock && busCableBlock.addInterface(world, pos, state, side)) {
            final PlayerEntity player = context.getPlayer();
            final ItemStack stack = context.getItem();

            if (player instanceof ServerPlayerEntity) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) player, pos, stack);
            }

            WorldUtils.playSound(world, pos, state.getSoundType(world, pos, player), SoundType::getPlaceSound);

            if (player == null || !player.abilities.isCreativeMode) {
                stack.shrink(1);
            }

            return ActionResultType.func_233537_a_(world.isRemote);
        }

        return ActionResultType.PASS;
    }
}
