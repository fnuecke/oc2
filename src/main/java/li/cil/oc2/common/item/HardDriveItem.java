package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;

public final class HardDriveItem extends BlockDeviceItem {
    public HardDriveItem(final Properties properties) {
        super(properties, 2 * Constants.MEGABYTE);
    }
}
