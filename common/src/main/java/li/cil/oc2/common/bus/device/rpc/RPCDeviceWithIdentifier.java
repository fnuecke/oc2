/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;

import java.util.UUID;

public record RPCDeviceWithIdentifier(UUID identifier, RPCDevice device) {
}
