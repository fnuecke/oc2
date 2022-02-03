/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.capabilities;

import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * This interface provides access to a list of {@link Player}s that are currently
 * using a terminal or similar provided by the owner of this capability.
 * <p>
 * For example, for computers and robots this is the list of players that currently have
 * the terminal UI opened.
 * <p>
 * Must be implemented by the {@link BlockEntity} or {@link Entity} that serves as the
 * context for device creation via {@link BlockDeviceQuery}s or {@link ItemDeviceQuery}s,
 * respectively.
 */
public interface TerminalUserProvider {
    /**
     * The list of players currently interacting with a terminal.
     *
     * @return the list of terminal users.
     */
    Iterable<Player> getTerminalUsers();
}
