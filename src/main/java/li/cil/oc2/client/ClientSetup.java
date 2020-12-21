package li.cil.oc2.client;

import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.render.tile.ComputerTileEntityRenderer;
import li.cil.oc2.common.init.Containers;
import li.cil.oc2.common.init.TileEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.screen.PlayerScreenHandler;

public final class ClientSetup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(Containers.COMPUTER_CONTAINER, ComputerContainerScreen::new);

        BlockEntityRendererRegistry.INSTANCE.register(TileEntities.COMPUTER_TILE_ENTITY, ComputerTileEntityRenderer::new);

        ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).register(ComputerTileEntityRenderer::registerComputerAtlasTextures);
    }
}
