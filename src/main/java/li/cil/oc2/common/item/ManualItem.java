package li.cil.oc2.common.item;

import li.cil.manual.api.ManualModel;
import li.cil.manual.api.ManualScreenStyle;
import li.cil.manual.api.ManualStyle;
import li.cil.manual.api.prefab.item.AbstractManualItem;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.client.manual.ModManualScreenStyle;
import li.cil.oc2.client.manual.ModManualStyle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class ManualItem extends AbstractManualItem {
    public ManualItem() {
        super(new Properties().tab(ItemGroup.COMMON));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected ManualModel getManualModel() {
        return Manuals.MANUAL.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ManualStyle getManualStyle() {
        return ModManualStyle.INSTANCE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ManualScreenStyle getScreenStyle() {
        return ModManualScreenStyle.INSTANCE;
    }
}
