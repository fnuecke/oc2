package li.cil.oc2.common.block;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.client.gui.TerminalScreen;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.container.ComputerContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public final class ComputerBlock extends HorizontalBlock {
    public ComputerBlock() {
        super(Properties.create(Material.IRON).sound(SoundType.METAL));
        setDefaultState(getStateContainer().getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
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
    public void neighborChanged(final BlockState state, final World world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof ComputerTileEntity) {
            final ComputerTileEntity busCable = (ComputerTileEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            throw new IllegalStateException();
        }

        final ComputerTileEntity computer = (ComputerTileEntity) tileEntity;
        if (player.isSneaking()) {
            if (!world.isRemote()) {
                computer.start();
            }
        } else {
            final boolean openContainer = false; // TODO
            if (openContainer) {
                if (!world.isRemote()) {
                    if (!(player instanceof ServerPlayerEntity)) {
                        throw new IllegalArgumentException();
                    }
                    openContainerScreen(tileEntity, (ServerPlayerEntity) player);
                }
            } else {
                if (world.isRemote()) {
                    openTerminalScreen(computer);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(HORIZONTAL_FACING);
    }

    ///////////////////////////////////////////////////////////////////

    private void openContainerScreen(final TileEntity tileEntity, final ServerPlayerEntity player) {
        NetworkHooks.openGui(player, new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return getTranslatedName();
            }

            @Override
            public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
                return new ComputerContainer(id, (ComputerTileEntity) tileEntity);
            }
        }, tileEntity.getPos());
    }

    private void openTerminalScreen(final ComputerTileEntity computer) {
        Minecraft.getInstance().displayGuiScreen(new TerminalScreen(computer, getTranslatedName()));
    }
}
