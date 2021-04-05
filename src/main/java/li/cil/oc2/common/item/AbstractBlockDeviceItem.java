package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;

public abstract class AbstractBlockDeviceItem extends ModItem {
    private static final String DATA_TAG_NAME = "data";

    ///////////////////////////////////////////////////////////////////

    private final ResourceLocation defaultData;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockDeviceItem(final Properties properties, final ResourceLocation defaultData) {
        super(properties.maxStackSize(1));
        this.defaultData = defaultData;
    }

    protected AbstractBlockDeviceItem(final ResourceLocation defaultData) {
        this(createProperties(), defaultData);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(DATA_TAG_NAME);

        ResourceLocation location = defaultData;
        if (!StringUtils.isNullOrEmpty(registryName)) {
            try {
                location = new ResourceLocation(registryName);
            } catch (final ResourceLocationException ignored) {
            }
        }

        return BlockDeviceDataRegistration.REGISTRY.get().getValue(location);
    }

    public ItemStack withData(final ItemStack stack, final BlockDeviceData data) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return stack;
        }

        final ResourceLocation key = BlockDeviceDataRegistration.REGISTRY.get().getKey(data);
        if (key == null) {
            return stack;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).putString(DATA_TAG_NAME, key.toString());

        return stack;
    }

    public ItemStack withData(final BlockDeviceData data) {
        return withData(new ItemStack(this), data);
    }

    @Override
    public ITextComponent getDisplayName(final ItemStack stack) {
        final BlockDeviceData data = getData(stack);
        if (data != null) {
            return new StringTextComponent("")
                    .append(super.getDisplayName(stack))
                    .appendString(" (")
                    .append(data.getDisplayName())
                    .appendString(")");
        } else {
            return super.getDisplayName(stack);
        }
    }
}
