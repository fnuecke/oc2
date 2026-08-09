/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.capabilities;

import li.cil.oc2.api.bus.device.ItemDevice;

import javax.annotation.Nullable;

/**
 * This interface provides interaction with the network bus.
 * <p>
 * Network connectors will check for this capability on blocks they are placed on.
 * If found, they will actively poll frames via {@link #readEthernetFrame()} and push
 * forwarded frames via {@link #writeEthernetFrame(NetworkInterface, byte[], int)}.
 * <p>
 * As with all capabilities, this capability can be provided by {@link ItemDevice}s.
 */
public interface NetworkInterface {
    /**
     * Tries to read an ethernet frame from this network interface.
     * <p>
     * The frame <em>should</em> be a Layer 2 Ethernet frame.
     * <p>
     * When no data is available, {@code null} should be returned.
     *
     * @return a pending frame or {@code null}.
     */
    @Nullable
    byte[] readEthernetFrame();

    /**
     * Tries to write an ethernet frame to this network interface.
     * <p>
     * The frame <em>should</em> be a Layer 2 Ethernet frame, but this is
     * not guaranteed. Implementations should not rely on this, and if relying
     * on this at least add appropriate validation and discard the frame otherwise.
     * <p>
     * If the device is not ready to receive data, it may ignore the call.
     * <p>
     * The {@code timeToLive} parameter is not to be confused with the IP protocol's
     * TTL field. This parameter is used when pushing frames through the network bus
     * to prevent infinite loops in case of cycles. Pure consumers can ignore this
     * argument. Any implementation forwarding directly (by pushing it to some other
     * {@link NetworkInterface} implementation) should call {@code writeEthernetFrame}
     * with the time to live reduced by some value, usually by one.
     *
     * @param source     the device that last forwarded the frame.
     * @param frame      the frame offered to the network interface.
     * @param timeToLive the number of hops remaining before the frame should be discarded.
     */
    void writeEthernetFrame(NetworkInterface source, byte[] frame, final int timeToLive);
}
