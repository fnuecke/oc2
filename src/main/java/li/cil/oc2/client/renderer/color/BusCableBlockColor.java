package li.cil.oc2.client.renderer.color;

import li.cil.oc2.common.tileentity.BusCableTileEntity;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BusCableBlockColor implements IBlockColor {
    @Override
    public int getColor(final BlockState state, @Nullable final IBlockDisplayReader level, @Nullable final BlockPos pos, final int tintIndex) {
        if (level == null || pos == null) {
            return 0;
        }

        final TileEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BusCableTileEntity) {
            final BlockState facade = ItemStackUtils.getBlockState(((BusCableTileEntity) blockEntity).getFacade());
            if (facade != null) {
                return Minecraft.getInstance().getBlockColors().getColor(facade, level, pos, tintIndex);
            }
        }

        return 0;
    }
}
