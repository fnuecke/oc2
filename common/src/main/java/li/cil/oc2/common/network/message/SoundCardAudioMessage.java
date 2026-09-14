/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.client.audio.SoundCardAudio;
import li.cil.oc2.common.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class SoundCardAudioMessage extends AbstractMessage {
    private static final int MAX_SAMPLES_SIZE = 32 * Constants.KILOBYTE;

    // --------------------------------------------------------------------- //

    private int streamId;
    private BlockPos pos;
    private int sampleRate;
    private byte[] samples;

    // --------------------------------------------------------------------- //

    public SoundCardAudioMessage(final int streamId, final BlockPos pos, final int sampleRate, final byte[] samples) {
        this.streamId = streamId;
        this.pos = pos;
        this.sampleRate = sampleRate;
        this.samples = samples;
    }

    public SoundCardAudioMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        streamId = buffer.readVarInt();
        pos = buffer.readBlockPos();
        sampleRate = buffer.readVarInt();
        samples = buffer.readByteArray(MAX_SAMPLES_SIZE);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(streamId);
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(sampleRate);
        buffer.writeByteArray(samples);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        SoundCardAudio.play(streamId, pos, sampleRate, samples);
    }
}
