/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

@RPCDeviceDescription(typeNames = {"jukebox"}, description = """
    Provided by jukeboxes connected to a [bus interface](../block/bus_interface.md). Times are in ticks.""")
@IODeviceDescription(name = "JUKEBX", description = """
    Times are two bytes, low byte first, capped at 65535.""")
public final class JukeboxDevice extends AbstractBlockDevice {
    private static final int IS_PLAYING_CODE = 1;
    private static final int GET_SONG_CODE = 2;
    private static final int GET_SONG_LENGTH_CODE = 3;
    private static final int GET_ELAPSED_CODE = 4;

    // --------------------------------------------------------------------- //

    public JukeboxDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets whether the jukebox is playing a song.",
        returnValueDescription = "whether a track is playing.")
    public boolean isPlaying() {
        return getPlayer().isPlaying();
    }

    @Callback(description = "Gets the song the jukebox is playing.",
        returnValueDescription = "the name of the song, such as `minecraft:cat`, if any.")
    @Nullable
    public String getSong() {
        final JukeboxSong song = getPlayer().getSong();
        if (song == null) {
            return null;
        }

        final ResourceLocation key = getServerLevel().registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getKey(song);
        return key == null ? null : key.toString();
    }

    @Callback(description = "Gets how long the song the jukebox is playing lasts.",
        returnValueDescription = "the length of the song.")
    public int getSongLength() {
        final JukeboxSong song = getPlayer().getSong();
        return song == null ? 0 : song.lengthInTicks();
    }

    @Callback(description = "Gets how long the jukebox has been playing the current song.",
        returnValueDescription = "the time since the song started.")
    public int getElapsed() {
        final JukeboxSongPlayer player = getPlayer();
        return player.isPlaying() ? (int) player.getTicksSinceSongStarted() : 0;
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = IS_PLAYING_CODE,
        description = "Gets whether the jukebox is playing a song.",
        resultsDescription = "one byte, `1` while a track is playing, `0` otherwise.")
    public void isPlaying(final IOOutputStream results) throws IOException {
        results.writeU8(isPlaying() ? 1 : 0);
    }

    @IOCallback(value = GET_SONG_CODE,
        description = "Gets the song the jukebox is playing.",
        resultsDescription = "the name of the song, such as `minecraft:cat`, if any. Read while `OCDAV` is set to read fully.")
    public void getSong(final IOOutputStream results) throws IOException {
        results.writeString(Objects.requireNonNullElse(getSong(), ""));
    }

    @IOCallback(value = GET_SONG_LENGTH_CODE,
        description = "Reads how long the song the jukebox is playing lasts.",
        resultsDescription = "two bytes, the length of the song.")
    public void getSongLength(final IOOutputStream results) throws IOException {
        results.writeU16(Math.min(getSongLength(), 0xFFFF));
    }

    @IOCallback(value = GET_ELAPSED_CODE,
        description = "Reads how long the jukebox has been playing the current song.",
        resultsDescription = "two bytes, the time since the song started.")
    public void getElapsed(final IOOutputStream results) throws IOException {
        results.writeU16(Math.min(getElapsed(), 0xFFFF));
    }

    // --------------------------------------------------------------------- //

    private JukeboxSongPlayer getPlayer() {
        return getBlockEntity(JukeboxBlockEntity.class).getSongPlayer();
    }
}
