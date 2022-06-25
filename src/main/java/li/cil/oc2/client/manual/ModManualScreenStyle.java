/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.manual;

import li.cil.manual.api.ManualScreenStyle;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModManualScreenStyle implements ManualScreenStyle {
    public static final ManualScreenStyle INSTANCE = new ModManualScreenStyle();

    @Override
    public ResourceLocation getWindowBackground() {
        return new ResourceLocation(API.MOD_ID, "textures/gui/manual/manual.png");
    }

    @Override
    public ResourceLocation getScrollButtonTexture() {
        return new ResourceLocation(API.MOD_ID, "textures/gui/manual/scroll_button.png");
    }

    @Override
    public ResourceLocation getTabButtonTexture() {
        return new ResourceLocation(API.MOD_ID, "textures/gui/manual/tab_button.png");
    }

    @Override
    public Rect2i getDocumentRect() {
        return new Rect2i(12, 12, 216, 232);
    }

    @Override
    public Rect2i getScrollBarRect() {
        return new Rect2i(236, 8, 12, 240);
    }

    @Override
    public Rect2i getScrollButtonRect() {
        return new Rect2i(0, 0, 12, 12);
    }

    @Override
    public Rect2i getTabAreaRect() {
        return new Rect2i(-52, 12, 52, 232);
    }

    @Override
    public Rect2i getTabRect() {
        return new Rect2i(0, 0, 64, 24);
    }

    @Override
    public int getTabOverlap() {
        return 0;
    }
}
