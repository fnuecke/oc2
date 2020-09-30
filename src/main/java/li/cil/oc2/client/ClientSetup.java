package li.cil.oc2.client;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    public static void run(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(OpenComputers.COMPUTER_CONTAINER.get(), ComputerContainerScreen::new);
    }
}
