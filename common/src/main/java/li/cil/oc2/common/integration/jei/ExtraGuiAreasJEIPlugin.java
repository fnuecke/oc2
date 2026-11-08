/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.jei;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.layout.UiContainerScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class ExtraGuiAreasJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "extra_gui_areas");
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(UiContainerScreen.class, new UiContainerScreenGuiContainerHandler());
    }

    private static final class UiContainerScreenGuiContainerHandler implements IGuiContainerHandler<UiContainerScreen<?>> {
        @Override
        public List<Rect2i> getGuiExtraAreas(final UiContainerScreen<?> screen) {
            return screen.getExtraAreas();
        }
    }
}
