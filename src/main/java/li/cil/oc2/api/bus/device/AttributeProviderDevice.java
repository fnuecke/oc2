package li.cil.oc2.api.bus.device;

import alexiil.mc.lib.attributes.AttributeList;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface AttributeProviderDevice {
    void addAllAttributes(AttributeList<?> attributeList, @Nullable Direction localSide);
}
