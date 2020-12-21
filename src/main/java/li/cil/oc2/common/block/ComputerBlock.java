package li.cil.oc2.common.block;

import li.cil.oc2.client.gui.TerminalScreen;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.container.ComputerContainer;
import li.cil.oc2.common.init.TileEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ComputerBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public ComputerBlock() {
        super(AbstractBlock.Settings.of(Material.METAL).sounds(BlockSoundGroup.METAL));
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(final ItemPlacementContext context) {
        return super.getDefaultState().with(FACING, context.getPlayerLookDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(final BlockView world) {
        return TileEntities.COMPUTER_TILE_ENTITY.instantiate();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborUpdate(final BlockState state, final World world, final BlockPos pos, final Block block, final BlockPos changedBlockPos, final boolean notify) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof ComputerTileEntity) {
            final ComputerTileEntity busCable = (ComputerTileEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockHitResult hit) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            throw new IllegalStateException();
        }

        final ComputerTileEntity computer = (ComputerTileEntity) tileEntity;
        if (player.isSneaking()) {
            if (!world.isClient()) {
                computer.start();
            }
        } else {
            final boolean openContainer = false; // TODO
            if (openContainer) {
                if (!world.isClient()) {
                    if (!(player instanceof ServerPlayerEntity)) {
                        throw new IllegalArgumentException();
                    }
                    openContainerScreen(tileEntity, (ServerPlayerEntity) player);
                }
            } else {
                if (world.isClient()) {
                    openTerminalScreen(computer);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    ///////////////////////////////////////////////////////////////////

    private void openContainerScreen(final BlockEntity tileEntity, final ServerPlayerEntity player) {
//        NetworkHooks.openGui(player, new INamedContainerProvider() {
//            @Override
//            public Text getDisplayName() {
//                return getTranslatedName();
//            }
//
//            @Override
//            public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
//                return new ComputerContainer(id, (ComputerTileEntity) tileEntity);
//            }
//        }, tileEntity.getPos());
    }

    private void openTerminalScreen(final ComputerTileEntity computer) {
        MinecraftClient.getInstance().openScreen(new TerminalScreen(computer, getName()));
    }
}
