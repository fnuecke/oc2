package li.cil.oc2.common.tags;

import li.cil.oc2.api.API;
import net.minecraft.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public final class BlockTags {
    public static final Tags.IOptionalNamedTag<Block> DEVICES = tag("devices");
    public static final Tags.IOptionalNamedTag<Block> CABLES = tag("cables");
    public static final Tags.IOptionalNamedTag<Block> WRENCH_BREAKABLE = tag("wrench_breakable");

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static Tags.IOptionalNamedTag<Block> tag(final String name) {
        return net.minecraft.tags.Tag.
        return net.minecraft.tags(new ResourceLocation(API.MOD_ID, name));
    }
}
