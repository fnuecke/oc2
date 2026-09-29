/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"lectern"}, description = """
    Provided by lecterns connected to a [bus interface](../block/bus_interface.md). Pages are numbered from `0`.""")
@IODeviceDescription(name = "LECTRN")
public final class LecternDevice extends AbstractBlockDevice {
    private static final int GET_PAGE_CODE = 1;
    private static final int GET_PAGE_COUNT_CODE = 2;

    // --------------------------------------------------------------------- //

    public LecternDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets the page the book on the lectern is open at.",
        returnValueDescription = "the page.")
    public int getPage() {
        return getLectern().getPage();
    }

    @Callback(description = "Gets how many pages the book on the lectern has.",
        returnValueDescription = "the number of pages, `0` when empty.")
    public int getPageCount() {
        return getLectern().pageCount;
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_PAGE_CODE,
        description = "Gets the page the book on the lectern is open at.",
        resultsDescription = "one byte, the page.")
    public void getPage(final IOOutputStream results) throws IOException {
        results.writeU8(getPage());
    }

    @IOCallback(value = GET_PAGE_COUNT_CODE,
        description = "Gets how many pages the book on the lectern has.",
        resultsDescription = "one byte, the number of pages, `0` when empty.")
    public void getPageCount(final IOOutputStream results) throws IOException {
        results.writeU8(getPageCount());
    }

    // --------------------------------------------------------------------- //

    private LecternBlockEntity getLectern() {
        return getBlockEntity(LecternBlockEntity.class);
    }
}
