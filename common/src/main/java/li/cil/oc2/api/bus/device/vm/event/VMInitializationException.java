/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.event;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * May be fired by devices while handling {@link VMInitializingEvent} to indicate that initialization failed.
 */
public final class VMInitializationException extends RuntimeException {
    private final Component message;

    ///////////////////////////////////////////////////////////////

    public VMInitializationException(final Component message) {
        this.message = message;
    }

    public VMInitializationException() {
        this.message = null;
    }

    ///////////////////////////////////////////////////////////////

    /**
     * The error message indicating why initialization failed.
     * <p>
     * This should be a human-readable message, as it may be displayed to the user.
     *
     * @return the error message.
     */
    public Optional<Component> getErrorMessage() {
        return Optional.ofNullable(message);
    }
}
