/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A macrboblock to slice group mapper that operates on prebuilt map, passed to
 * it in the constructor
 *
 * @author The JCodec project
 */
public final class PrebuiltMBlockMapper implements Mapper {
    private final MBToSliceGroupMap map;
    private final int firstMBInSlice;
    private final int groupId;
    private final int picWidthInMbs;
    private final int indexOfFirstMb;

    public PrebuiltMBlockMapper(final MBToSliceGroupMap map, final int firstMBInSlice, final int picWidthInMbs) {
        this.map = map;
        this.firstMBInSlice = firstMBInSlice;
        this.groupId = map.groups()[firstMBInSlice];
        this.picWidthInMbs = picWidthInMbs;
        this.indexOfFirstMb = map.indices()[firstMBInSlice];
    }

    public int getAddress(final int mbIndex) {
        return map.inverse()[groupId][mbIndex + indexOfFirstMb];
    }

    public boolean leftAvailable(final int mbIndex) {
        final int mbAddr = map.inverse()[groupId][mbIndex + indexOfFirstMb];
        final int leftMBAddr = mbAddr - 1;

        return !((leftMBAddr < firstMBInSlice) || ((mbAddr % picWidthInMbs) == 0) || (map.groups()[leftMBAddr] != groupId));
    }

    public boolean topAvailable(final int mbIndex) {
        final int mbAddr = map.inverse()[groupId][mbIndex + indexOfFirstMb];
        final int topMBAddr = mbAddr - picWidthInMbs;

        return !((topMBAddr < firstMBInSlice) || (map.groups()[topMBAddr] != groupId));
    }

    public int getMbX(final int index) {
        return getAddress(index) % picWidthInMbs;
    }

    public int getMbY(final int index) {
        return getAddress(index) / picWidthInMbs;
    }

    public boolean topRightAvailable(final int mbIndex) {
        final int mbAddr = map.inverse()[groupId][mbIndex + indexOfFirstMb];
        final int topRMBAddr = mbAddr - picWidthInMbs + 1;

        return !((topRMBAddr < firstMBInSlice) || (((mbAddr + 1) % picWidthInMbs) == 0) || (map.groups()[topRMBAddr] != groupId));
    }

    public boolean topLeftAvailable(final int mbIndex) {
        final int mbAddr = map.inverse()[groupId][mbIndex + indexOfFirstMb];
        final int topLMBAddr = mbAddr - picWidthInMbs - 1;

        return !((topLMBAddr < firstMBInSlice) || ((mbAddr % picWidthInMbs) == 0) || (map.groups()[topLMBAddr] != groupId));
    }
}
