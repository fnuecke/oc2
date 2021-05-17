package li.cil.oc2.api.capabilities;

import net.minecraft.entity.player.PlayerEntity;

/**
 * This interface provides access to a list of {@link PlayerEntity}s that are currently
 * using a terminal or similar provided by the owner of this capability.
 * <p>
 * For example, for computers and robots this is the list of players that currently have
 * the terminal UI opened.
 */
public interface TerminalUserProvider {
    /**
     * The list of players currently interacting with a terminal.
     *
     * @return the list of terminal users.
     */
    Iterable<PlayerEntity> getTerminalUsers();
}
