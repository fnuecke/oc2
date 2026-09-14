/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.audio;

import dev.architectury.event.events.client.ClientTickEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import li.cil.oc2.common.bus.device.vm.item.SoundCardDevice;
import li.cil.oc2.common.util.MuLaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.nio.ShortBuffer;
import java.util.Iterator;

public final class SoundCardAudio {
    private static final int MAX_STREAMS = 4;
    private static final int PREBUFFER_MILLIS = 150;
    private static final int MAX_QUEUED_MILLIS = 750;
    private static final long IDLE_TIMEOUT_MILLIS = 2000;

    private static final Int2ObjectMap<Stream> STREAMS = new Int2ObjectOpenHashMap<>();

    private static ShortBuffer decodeBuffer = BufferUtils.createShortBuffer(4096);
    private static boolean isContextAvailable;
    private static WeakReference<ClientLevel> lastLevel = new WeakReference<>(null);

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ClientTickEvent.CLIENT_POST.register(SoundCardAudio::handleClientTick);
    }

    public static void handleContextCreated() {
        isContextAvailable = true;
    }

    public static void handleContextDestroyed() {
        releaseAll();
        isContextAvailable = false;
    }

    public static void play(final int streamId, final BlockPos pos, final int sampleRate, final byte[] samples) {
        if (!isContextAvailable || samples.length == 0) {
            return;
        }

        Stream stream = STREAMS.get(streamId);
        if (stream != null && stream.sampleRate != sampleRate) {
            // OpenAL does not accept buffers of different formats in one source's queue.
            stream.release();
            STREAMS.remove(streamId);
            stream = null;
        }

        if (stream == null) {
            if (STREAMS.size() >= MAX_STREAMS) {
                return;
            }
            stream = Stream.create(sampleRate);
            if (stream == null) {
                return;
            }
            STREAMS.put(streamId, stream);
        }

        stream.enqueue(pos, samples);
    }

    // --------------------------------------------------------------------- //

    private static void handleClientTick(final Minecraft minecraft) {
        if (minecraft.level != lastLevel.get()) {
            lastLevel = new WeakReference<>(minecraft.level);
            releaseAll();
        }

        if (STREAMS.isEmpty()) {
            return;
        }

        final boolean isPaused = minecraft.isPaused();
        final float gain = minecraft.options.getSoundSourceVolume(SoundSource.RECORDS);
        final long now = System.currentTimeMillis();

        final Iterator<Stream> iterator = STREAMS.values().iterator();
        while (iterator.hasNext()) {
            final Stream stream = iterator.next();
            if (now - stream.lastReceivedAt > IDLE_TIMEOUT_MILLIS) {
                stream.release();
                iterator.remove();
            } else {
                stream.update(now, isPaused, gain);
            }
        }
    }

    private static void releaseAll() {
        for (final Stream stream : STREAMS.values()) {
            stream.release();
        }
        STREAMS.clear();
    }

    private static ShortBuffer decode(final byte[] samples) {
        if (decodeBuffer.capacity() < samples.length) {
            decodeBuffer = BufferUtils.createShortBuffer(samples.length);
        }

        decodeBuffer.clear();
        for (final byte sample : samples) {
            decodeBuffer.put(MuLaw.decode(sample));
        }
        decodeBuffer.flip();

        return decodeBuffer;
    }

    // --------------------------------------------------------------------- //

    private static final class Stream {
        private final int source;
        private final int sampleRate;
        private final IntArrayFIFOQueue queuedBuffers = new IntArrayFIFOQueue();
        private final IntArrayFIFOQueue queuedBufferSizes = new IntArrayFIFOQueue();
        private int queuedSamples;
        private long lastReceivedAt;

        @Nullable
        static Stream create(final int sampleRate) {
            final int source = AL10.alGenSources();
            if (!AL10.alIsSource(source)) {
                return null;
            }

            // Match Minecraft's linear attenuation setup.
            AL10.alSourcei(source, AL10.AL_DISTANCE_MODEL, AL11.AL_LINEAR_DISTANCE);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, SoundCardDevice.AUDIBLE_DISTANCE);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0);

            return new Stream(source, sampleRate);
        }

        private Stream(final int source, final int sampleRate) {
            this.source = source;
            this.sampleRate = sampleRate;
        }

        void enqueue(final BlockPos pos, final byte[] samples) {
            lastReceivedAt = System.currentTimeMillis();

            // A source that ran dry is stopped, and a stopped source reports every queued buffer as
            // processed, including buffers queued after it stopped. Reset it first, or the next
            // reclaim would delete the new audio unplayed and the source would never start again.
            if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED) {
                reclaimProcessedBuffers();
                AL10.alSourceRewind(source);
            }

            // This far behind, playing more only grows the delay. Drop it instead.
            if (queuedSamples + samples.length > millisToSamples(MAX_QUEUED_MILLIS)) {
                return;
            }

            final int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, decode(samples), sampleRate);
            AL10.alSourceQueueBuffers(source, buffer);
            queuedBuffers.enqueue(buffer);
            queuedBufferSizes.enqueue(samples.length);
            queuedSamples += samples.length;

            AL10.alSource3f(source, AL10.AL_POSITION, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
        }

        void update(final long now, final boolean isPaused, final float gain) {
            reclaimProcessedBuffers();
            AL10.alSourcef(source, AL10.AL_GAIN, gain);

            final int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            if (isPaused) {
                // A paused single player game pauses the server too, so no audio is missing.
                lastReceivedAt = now;
                if (state == AL10.AL_PLAYING) {
                    AL10.alSourcePause(source);
                }
            } else if (state == AL10.AL_PAUSED) {
                AL10.alSourcePlay(source);
            } else if (state != AL10.AL_PLAYING && queuedSamples > 0
                && (queuedSamples >= millisToSamples(PREBUFFER_MILLIS) || now - lastReceivedAt > PREBUFFER_MILLIS)) {
                // Start once enough is queued to compensate network jitter, or once nothing more is
                // coming, so the tail of a sound still plays.
                AL10.alSourcePlay(source);
            }
        }

        void release() {
            AL10.alSourceStop(source);
            AL10.alSourcei(source, AL10.AL_BUFFER, 0);
            while (!queuedBuffers.isEmpty()) {
                AL10.alDeleteBuffers(queuedBuffers.dequeueInt());
            }
            AL10.alDeleteSources(source);
        }

        private void reclaimProcessedBuffers() {
            for (int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED); processed > 0; processed--) {
                AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(source));
                queuedBuffers.dequeueInt();
                queuedSamples -= queuedBufferSizes.dequeueInt();
            }
        }

        private int millisToSamples(final int millis) {
            return sampleRate * millis / 1000;
        }
    }

    // --------------------------------------------------------------------- //

    private SoundCardAudio() {
    }
}
