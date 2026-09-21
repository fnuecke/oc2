/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

public final class RedstoneInterfaceDocumentation {
    public static final String DEVICE = """
        Provided by the [redstone interface](../block/redstone_interface.md) block and the [redstone interface card](../item/redstone_interface_card.md).

        ### Sides
        The side parameter in the following methods comes in two forms.

        Relative sides turn with the block, or with the computer holding the card. Each face of the redstone interface block has an indicator for convenience; the primary face is the one with a single marking. When looking at the primary face:
        - `front` is the face we are looking at.
        - `back` is the face behind the block.
        - `left` is the face to our left.
        - `right` is the face to our right.

        Absolute sides always mean the same direction in the world, no matter how the block is placed: `north`, `south`, `west` and `east`.

        `up` and `down` are the top and bottom faces, and mean the same thing either way.

        Sides may also be given as a number instead of a name. Numbers are relative: `0` is `down`, `1` is `up`, `2` is `back`, `3` is `front`, `4` is `left` and `5` is `right`.

        ### Events
        The redstone interface block sends `redstoneChanged` when the received signal on a side changes, so a program can wait for it instead of polling. The card sends no events. For example:
        `local e = r:waitEvent(nil, "redstoneChanged")`
        `print(e.data.side, e.data.value)`
        - `side` is the relative name of the side, as in the "Sides" section.
        - `value` is the new signal strength.

        The device's own output counts towards the received signal, so setting an output may send this event as well.""";

    public static final String IO_DEVICE = """
        Sides are numbered as in the "Sides" section above, and levels are in [0, 15].

        `REDSTN.Z80` on the CP/M boot disk is an example consumer of the API. The [mid-level API](../mlapi.md) entry explains how to build and run it.""";

    public static final String SIDE = "the side, by name (`front`, `back`, `left`, `right`, `up`, `down`, `north`, `south`, `west`, `east`) or by relative index.";

    public static final String GET_REDSTONE_INPUT = "Gets the received redstone signal for the specified side. The device's own output on that side counts towards the received signal.";
    public static final String GET_REDSTONE_INPUT_RESULT = "the current input signal strength.";
    public static final String GET_REDSTONE_OUTPUT = "Gets the emitted redstone signal for the specified side. This is the value last set via setRedstoneOutput().";
    public static final String GET_REDSTONE_OUTPUT_RESULT = "the current output signal strength.";
    public static final String SET_REDSTONE_OUTPUT = "Sets the emitted redstone signal for the specified side.";
    public static final String SET_REDSTONE_OUTPUT_VALUE = "the signal strength to set, in the range of [0, 15]. Values outside are clamped.";

    public static final String GET_REDSTONE_INPUT_IO = "Reads the level received on that side.";
    public static final String GET_REDSTONE_OUTPUT_IO = "Reads the level currently being sent on that side.";
    public static final String SET_REDSTONE_OUTPUT_IO = "Sets the level sent on that side.";
    public static final String SIDE_IO = "one byte, the side.";
    public static final String LEVEL_IO = "one byte, the level.";
    public static final String SIDE_AND_LEVEL_IO = "two bytes, the side and the level.";

    private RedstoneInterfaceDocumentation() {
    }
}
