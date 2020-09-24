package li.cil.circuity.vm.evdev;

/**
 * Linux evdev <a href="https://www.kernel.org/doc/html/latest/input/event-codes.html#input-event-codes">input event types</a>.
 * <p>
 * Numeric values from Linux kernel: include/uapi/linux/input-event-codes.h
 */
public final class EvdevEvents {
    /**
     * Used as markers to separate events. Events may be separated in time or in space,
     * such as with the multitouch protocol.
     */
    public static final int EV_SYN = 0x00;
    /**
     * Used to describe state changes of keyboards, buttons, or other key-like devices.
     */
    public static final int EV_KEY = 0x01;
    /**
     * Used to describe relative axis value changes, e.g. moving the mouse 5 units to the left.
     */
    public static final int EV_REL = 0x02;
    /**
     * Used to describe absolute axis value changes, e.g. describing the coordinates of a touch on a touchscreen.
     */
    public static final int EV_ABS = 0x03;
    /**
     * Used to describe miscellaneous input data that do not fit into other types.
     */
    public static final int EV_MSC = 0x04;
    /**
     * Used to describe binary state input switches.
     */
    public static final int EV_SW = 0x05;
    /**
     * Used to turn LEDs on devices on and off.
     */
    public static final int EV_LED = 0x11;
    /**
     * Used to output sound to devices.
     */
    public static final int EV_SND = 0x12;
    /**
     * Used for autorepeating devices.
     */
    public static final int EV_REP = 0x14;
    /**
     * Used to send force feedback commands to an input device.
     */
    public static final int EV_FF = 0x15;
    /**
     * A special type for power button and switch input.
     */
    public static final int EV_PWR = 0x16;
    /**
     * Used to receive force feedback device status.
     */
    public static final int EV_FF_STATUS = 0x17;
}
