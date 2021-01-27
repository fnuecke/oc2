package li.cil.oc2.common.item;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class BusInterfaceItem extends ModItem {
    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final World world = context.getWorld();
        final BlockPos pos = context.getPos();
        final BlockState state = world.getBlockState(pos);

        final BusCableBlock busCableBlock = Blocks.BUS_CABLE.get();
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

            // TODO Store in cable whether cable itself is present, i.e. allow bus blocks that are just plugs.
//            if (neighborState.isReplaceable(new BlockItemUseContext(context))) {
//                world.setBlockState(neighborPos, Blocks.BUS_CABLE.get().getDefaultState());
//            }

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
