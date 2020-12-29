package li.cil.oc2.api.bus.device.data;

import li.cil.oc2.api.API;
import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Implementations of this interface that are registered with the registry for
 * this type can be used as read-only base block devices for read-write block
 * devices.
 * <p>
 * This is used for the built-in Linux root file-system, for example.
 * <p>
 * To make use of registered implementations, a hard drive item with the
 * string tag {@code oc2.base} referencing the implementation's registry id
 * must be created. For example, if the implementation's registry name is
 * {@code my_mod:my_block_device}:
 * <pre>
 * /give ? oc2:hard_drive{oc2:{base:"my_mod:my_block_device"}}
 * </pre>
 */
public interface BaseBlockDevice extends IForgeRegistryEntry<BaseBlockDevice> {
    /**
     * The registry name of the registry holding block device bases.
     */
    ResourceLocation REGISTRY = new ResourceLocation(API.MOD_ID, "base_block_device");

    /**
     * Gets the read-only base block device this implementation describes.
     *
     * @return the block device.
     */
    BlockDevice get();

    /**
     * The display name of this block device base. May be shown in the tooltip
     * of item devices using this base.
     *
     * @return the display name of this block device.
     */
    ITextComponent getName();
}
