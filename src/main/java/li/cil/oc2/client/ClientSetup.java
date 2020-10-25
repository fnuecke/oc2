package li.cil.oc2.client;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.render.tile.ComputerTileEntityRenderer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    public static void run(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(OpenComputers.COMPUTER_CONTAINER.get(), ComputerContainerScreen::new);

        ClientRegistry.bindTileEntityRenderer(OpenComputers.COMPUTER_TILE_ENTITY.get(), ComputerTileEntityRenderer::new);
    }
}
