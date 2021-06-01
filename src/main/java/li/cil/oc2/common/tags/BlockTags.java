package li.cil.oc2.common.tags;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;

public final class BlockTags {
    public static final Tag<Block> DEVICES = tag("devices");
    public static final Tag<Block> CABLES = tag("cables");
    public static final Tag<Block> WRENCH_BREAKABLE = tag("wrench_breakable");

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static Tag<Block> tag(final String name) {
        return TagRegistry.block(new ResourceLocation(API.MOD_ID, name));
    }
}
