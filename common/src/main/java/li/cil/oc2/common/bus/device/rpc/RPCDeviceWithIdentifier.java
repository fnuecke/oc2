/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import java.util.UUID;

public record RPCDeviceWithIdentifier(UUID identifier, RPCDeviceGroup device) {
}
