/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.capabilities;

/**
 * This interface may be provided as a capability by item components to signal
 * to the containing {@link net.minecraft.world.level.block.entity.BlockEntity} that they wish
 * to emit a redstone signal. This is used by the built-in redstone interface
 * card, for example.
 */
public interface RedstoneEmitter {
    /**
     * Returns the redstone output level for the side this interface was returned for.
     *
     * @return the redstone output level.
     */
    int getRedstoneOutput();
}
