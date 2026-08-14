/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultipartMessageTests {
    @BeforeAll
    public static void setupAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        MultipartMessage.registerMessage(PayloadMessage.class, PayloadMessage::new);
    }

    @BeforeEach
    public void setupEach() {
        PayloadMessage.RECEIVED.clear();
    }

    @Test
    public void singleSenderIsReassembledInOrder() {
        final NetworkManager.PacketContext context = contextForPlayer(UUID.randomUUID());

        dispatch(context, 1, false, "hello ");
        dispatch(context, 1, true, "world");

        assertEquals(List.of("hello world"), PayloadMessage.RECEIVED);
    }

    @Test
    public void concurrentSendersDoNotShareABuffer() {
        final NetworkManager.PacketContext steve = contextForPlayer(UUID.randomUUID());
        final NetworkManager.PacketContext herobrine = contextForPlayer(UUID.randomUUID());

        dispatch(steve, 1, false, "steve-");
        dispatch(herobrine, 1, false, "herobrine-");
        dispatch(herobrine, 1, true, "payload");
        dispatch(steve, 1, true, "payload");

        assertEquals(List.of("herobrine-payload", "steve-payload"), PayloadMessage.RECEIVED);
    }

    @Test
    public void oneSendersInterleavedTransfersDoNotShareABuffer() {
        final NetworkManager.PacketContext context = contextForPlayer(UUID.randomUUID());

        dispatch(context, 1, false, "first-");
        dispatch(context, 2, false, "second-");
        dispatch(context, 1, true, "done");
        dispatch(context, 2, true, "complete");

        assertEquals(List.of("first-done", "second-complete"), PayloadMessage.RECEIVED);
    }

    @Test
    public void finalPartIsExplicitNotInferredFromSize() {
        final NetworkManager.PacketContext context = contextForPlayer(UUID.randomUUID());
        final String maxSizeChunk = "x".repeat(32756 /* MAX_PAYLOAD_SIZE - HEADER_SIZE */);

        dispatch(context, 1, true, maxSizeChunk);

        assertEquals(List.of(maxSizeChunk), PayloadMessage.RECEIVED);
    }

    // ------------------------------------------------------------- //

    private static void dispatch(final NetworkManager.PacketContext context,
                                 final int multipartMessageId,
                                 final boolean isFinalPart,
                                 final String payload) {
        final RegistryFriendlyByteBuf inner = newBuffer();
        new PayloadMessage(payload).toBytes(inner);
        final byte[] data = new byte[inner.readableBytes()];
        inner.readBytes(data);

        final int messageId = MultipartMessage.messageIdOf(PayloadMessage.class);
        final RegistryFriendlyByteBuf wire = newBuffer();
        new MultipartMessage(messageId, multipartMessageId, isFinalPart, data).toBytes(wire);

        new MultipartMessage(wire).handle(context);
    }

    private static RegistryFriendlyByteBuf newBuffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static NetworkManager.PacketContext contextForPlayer(final UUID uuid) {
        final Player player = mock(Player.class);
        when(player.getUUID()).thenReturn(uuid);

        final NetworkManager.PacketContext context = mock(NetworkManager.PacketContext.class);
        when(context.getPlayer()).thenReturn(player);
        when(context.registryAccess()).thenReturn(RegistryAccess.EMPTY);
        return context;
    }

    // ------------------------------------------------------------- //

    public static final class PayloadMessage extends AbstractMessage {
        public static final List<String> RECEIVED = new ArrayList<>();

        private byte[] payload;

        public PayloadMessage(final String payload) {
            this.payload = payload.getBytes(StandardCharsets.UTF_8);
        }

        public PayloadMessage(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        public void fromBytes(final RegistryFriendlyByteBuf buffer) {
            payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
        }

        @Override
        public void toBytes(final RegistryFriendlyByteBuf buffer) {
            buffer.writeBytes(payload);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            RECEIVED.add(new String(payload, StandardCharsets.UTF_8));
        }
    }
}
