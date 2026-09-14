/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataClientView;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.DatapackBlockDeviceData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BlockDeviceDataMessage extends AbstractMessage {
    private List<BlockDeviceDataClientView> values;

    // --------------------------------------------------------------------- //

    public BlockDeviceDataMessage(final Collection<DatapackBlockDeviceData> values) {
        this.values = new ArrayList<>(values.size());
        for (final DatapackBlockDeviceData value : values) {
            this.values.add(new BlockDeviceDataClientView(value.getLocation(),
                value.getCapacity(), value.getDisplayName(), value.getColor()));
        }
    }

    public BlockDeviceDataMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        final int count = buffer.readVarInt();
        values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final var location = buffer.readResourceLocation();
            final var capacity = buffer.readVarLong();
            final var displayName = ComponentSerialization.STREAM_CODEC.decode(buffer);
            final var color = buffer.readNullable(b -> b.readEnum(DyeColor.class));
            values.add(new BlockDeviceDataClientView(location, capacity, displayName, color));
        }
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(values.size());
        for (final BlockDeviceDataClientView value : values) {
            buffer.writeResourceLocation(value.getLocation());
            buffer.writeVarLong(value.getCapacity());
            ComponentSerialization.STREAM_CODEC.encode(buffer, value.getDisplayName());
            buffer.writeNullable(value.getColor(), FriendlyByteBuf::writeEnum);
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        BlockDeviceDataRegistry.setClientViews(values);
    }
}
