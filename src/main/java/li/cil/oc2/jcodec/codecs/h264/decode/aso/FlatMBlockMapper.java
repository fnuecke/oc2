/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A block map that that maps macroblocks sequentially in scan order
 *
 * @author The JCodec project
 */
public record FlatMBlockMapper(int frameWidthInMbs, int firstMBAddr) implements Mapper {
    public boolean leftAvailable(final int index) {
        final int mbAddr = index + firstMBAddr;
        final boolean atTheBorder = mbAddr % frameWidthInMbs == 0;
        return !atTheBorder && (mbAddr > firstMBAddr);
    }

    public boolean topAvailable(final int index) {
        final int mbAddr = index + firstMBAddr;
        return mbAddr - frameWidthInMbs >= firstMBAddr;
    }

    public int getAddress(final int index) {
        return firstMBAddr + index;
    }

    public int getMbX(final int index) {
        return getAddress(index) % frameWidthInMbs;
    }

    public int getMbY(final int index) {
        return getAddress(index) / frameWidthInMbs;
    }

    public boolean topRightAvailable(final int index) {
        final int mbAddr = index + firstMBAddr;
        final boolean atTheBorder = (mbAddr + 1) % frameWidthInMbs == 0;
        return !atTheBorder && mbAddr - frameWidthInMbs + 1 >= firstMBAddr;
    }

    public boolean topLeftAvailable(final int index) {
        final int mbAddr = index + firstMBAddr;
        final boolean atTheBorder = mbAddr % frameWidthInMbs == 0;
        return !atTheBorder && mbAddr - frameWidthInMbs - 1 >= firstMBAddr;
    }
}
