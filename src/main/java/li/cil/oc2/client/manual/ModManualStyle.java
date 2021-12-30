package li.cil.oc2.client.manual;

import li.cil.manual.api.ManualStyle;
import li.cil.manual.api.render.FontRenderer;
import li.cil.oc2.client.renderer.font.MonospaceFontRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModManualStyle implements ManualStyle {
    public static final ManualStyle INSTANCE = new ModManualStyle();

    @Override
    public int getRegularTextColor() {
        return 0xFFDDDDDD;
    }

    @Override
    public int getMonospaceTextColor() {
        return 0xFF37FF99;
    }

    @Override
    public int getRegularLinkColor() {
        return 0xFF9CC6E7;
    }

    @Override
    public int getHoveredLinkColor() {
        return 0xFFBADCF7;
    }

    @Override
    public int getRegularDeadLinkColor() {
        return 0xFFFF3755;
    }

    @Override
    public int getHoveredDeadLinkColor() {
        return 0xFFFF8497;
    }

    @Override
    public FontRenderer getMonospaceFont() {
        return MonospaceFontRenderer.INSTANCE;
    }

    @Override
    public SoundEvent getPageChangeSound() {
        return SoundEvents.UI_BUTTON_CLICK;
    }
}
