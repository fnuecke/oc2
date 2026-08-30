/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

public record DeviceLocation(Kind kind, int slot) {
    public enum Kind {
        SLOT,
        BUS,
        UNSPECIFIED,
    }

    public static final DeviceLocation BUS = new DeviceLocation(Kind.BUS, -1);
    public static final DeviceLocation UNSPECIFIED = new DeviceLocation(Kind.UNSPECIFIED, -1);

    public static DeviceLocation slot(final int index) {
        return new DeviceLocation(Kind.SLOT, index);
    }
}
