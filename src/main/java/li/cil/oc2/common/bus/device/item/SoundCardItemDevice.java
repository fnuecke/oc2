package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.Location;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SoundCardItemDevice extends AbstractItemRPCDevice {
    private static final int COOLDOWN_IN_TICKS = 2 * Constants.SECONDS_TO_TICKS;
    private static final int MAX_FIND_RESULTS = 25;

    ///////////////////////////////////////////////////////////////////

    private final Supplier<Optional<Location>> location;
    private long gameTimeCooldownExpiresAt;

    ///////////////////////////////////////////////////////////////////

    public SoundCardItemDevice(final ItemStack identity, final Supplier<Optional<Location>> location) {
        super(identity, "sound");
        this.location = location;
    }

    ///////////////////////////////////////////////////////////////////

    @Callback
    public void playSound(@Nullable @Parameter("name") final String name) {
        if (name == null) throw new IllegalArgumentException();

        location.get().ifPresent(location -> {
            final IWorld level = location.world;
            if (!(level instanceof ServerWorld)) {
                return;
            }

            final long gameTime = ((ServerWorld) level).getGameTime();
            if (gameTime < gameTimeCooldownExpiresAt) {
                return;
            }
            gameTimeCooldownExpiresAt = gameTime + COOLDOWN_IN_TICKS;

            final SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(name));
            if (soundEvent == null) throw new IllegalArgumentException("Sound not found.");
            level.playSound(null, location.pos, soundEvent, SoundCategory.BLOCKS, 1, 1);
        });
    }

    @Callback
    public List<String> findSound(@Nullable @Parameter("name") final String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException();

        final ArrayList<String> matches = new ArrayList<>();

        for (final ResourceLocation key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
            final String keyName = key.toString();
            if (keyName.contains(name)) {
                matches.add(keyName);
                if (matches.size() >= MAX_FIND_RESULTS) {
                    break;
                }
            }
        }

        return matches;
    }
}
