package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.common.container.ComputerContainer;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;

public final class Containers {
    public static final ScreenHandlerType<ComputerContainer> COMPUTER_CONTAINER = ScreenHandlerRegistry.registerExtended(Constants.COMPUTER_BLOCK_NAME, ComputerContainer::create);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
