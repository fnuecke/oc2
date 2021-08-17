package li.cil.oc2.common.network.message;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public abstract class AbstractMessage {
    protected static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    protected AbstractMessage() {
    }

    protected AbstractMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final AbstractMessage message, final Supplier<NetworkEvent.Context> contextSupplier) {
        message.handleMessage(contextSupplier);
        return true;
    }

    public abstract void fromBytes(final PacketBuffer buffer);

    public abstract void toBytes(final PacketBuffer buffer);

    ///////////////////////////////////////////////////////////////////

    protected void handleMessage(final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleMessage(context));
    }

    protected void handleMessage(final NetworkEvent.Context context) {
        throw new NotImplementedException("Message implements neither asynchronous nor synchronous handleMessage() method.");
    }
}
