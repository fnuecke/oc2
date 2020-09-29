package li.cil.oc2.common.block;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.common.container.ComputerContainer;
import li.cil.oc2.common.tile.ComputerTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public final class ComputerBlock extends Block {
    public ComputerBlock() {
        super(Properties.create(Material.IRON).sound(SoundType.METAL));
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return OpenComputers.COMPUTER_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        if (!world.isRemote()) {
            if (!(player instanceof ServerPlayerEntity)) {
                throw new IllegalArgumentException();
            }

            final TileEntity tileEntity = world.getTileEntity(pos);
            if (!(tileEntity instanceof ComputerTileEntity)) {
                throw new IllegalStateException();
            }

            NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName() {
                    return new TranslationTextComponent("blah");
                }

                @Override
                public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
                    return new ComputerContainer(id, tileEntity);
                }
            }, tileEntity.getPos());
        }
        return ActionResultType.SUCCESS;
    }
}
