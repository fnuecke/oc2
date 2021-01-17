package li.cil.oc2.common.item;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BusInterfaceItem extends Item {
    public BusInterfaceItem(final Properties properties) {
        super(properties);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void addInformation(final ItemStack stack, @Nullable final World worldIn, final List<ITextComponent> tooltip, final ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        TooltipUtils.addDescription(stack, tooltip);
    }

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final World world = context.getWorld();
        final BlockPos pos = context.getPos();
        final BlockState state = world.getBlockState(pos);

        final BusCableBlock busCableBlock = Blocks.BUS_CABLE_BLOCK.get();
        if (state.getBlock() == busCableBlock) {
            final Vector3d localHitPos = context.getHitVec().subtract(Vector3d.copyCentered(pos));
            final Direction side = Direction.getFacingFromVector(localHitPos.x, localHitPos.y, localHitPos.z);
            if (busCableBlock.addPlug(world, pos, state, side)) {
                context.getItem().shrink(1);
                return ActionResultType.SUCCESS;
            }
        } else {
            final BlockPos neighborPos = pos.offset(context.getFace());
            final BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() == busCableBlock) {
                final Direction side = context.getFace().getOpposite();
                if (busCableBlock.addPlug(world, neighborPos, neighborState, side)) {
                    context.getItem().shrink(1);
                    return ActionResultType.SUCCESS;
                }
            }
        }

        return super.onItemUse(context);
    }
}
