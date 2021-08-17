package li.cil.oc2.client.manual;

import li.cil.manual.api.ManualScreenStyle;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.util.ResourceLocation;
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
    public Rectangle2d getDocumentRect() {
        return new Rectangle2d(12, 12, 216, 232);
    }

    @Override
    public Rectangle2d getScrollBarRect() {
        return new Rectangle2d(236, 8, 12, 240);
    }

    @Override
    public Rectangle2d getScrollButtonRect() {
        return new Rectangle2d(0, 0, 12, 12);
    }

    @Override
    public Rectangle2d getTabAreaRect() {
        return new Rectangle2d(-52, 12, 52, 232);
    }

    @Override
    public Rectangle2d getTabRect() {
        return new Rectangle2d(0, 0, 64, 24);
    }

    @Override
    public int getTabOverlap() {
        return 0;
    }
}
