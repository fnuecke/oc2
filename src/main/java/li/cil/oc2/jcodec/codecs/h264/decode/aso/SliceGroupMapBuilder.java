/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A helper class that builds macroblock to slice group maps needed by ASO
 * (Arbitrary Slice Order)
 *
 * @author The JCodec project
 */
public final class SliceGroupMapBuilder {
    /**
     * Interleaved slice group map. Each slice group fills a number of cells
     * equal to the appropriate run length, then followed by the next slice
     * group.
     * <p>
     * Example:
     * <p>
     * 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1,
     */
    public static int[] buildInterleavedMap(final int picWidthInMbs, final int picHeightInMbs, final int[] runLength) {
        final int numSliceGroups = runLength.length;
        final int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        final int[] groups = new int[picSizeInMbs];

        int i = 0;
        do {
            for (int iGroup = 0; iGroup < numSliceGroups && i < picSizeInMbs; i += runLength[iGroup++]) {
                for (int j = 0; j < runLength[iGroup] && i + j < picSizeInMbs; j++) {
                    groups[i + j] = iGroup;
                }
            }
        } while (i < picSizeInMbs);

        return groups;
    }

    /**
     * A dispersed map. Every odd line starts from the (N / 2)th group
     * <p>
     * Example:
     * <p>
     * 0, 1, 2, 3, 0, 1, 2, 3 2, 3, 0, 1, 2, 3, 0, 1 0, 1, 2, 3, 0, 1, 2, 3 2,
     * 3, 0, 1, 2, 3, 0, 1 0, 1, 2, 3, 0, 1, 2, 3 2, 3, 0, 1, 2, 3, 0, 1 0, 1,
     * 2, 3, 0, 1, 2, 3 2, 3, 0, 1, 2, 3, 0, 1 0, 1, 2, 3, 0, 1, 2, 3 2, 3, 0,
     * 1, 2, 3, 0, 1
     */
    public static int[] buildDispersedMap(final int picWidthInMbs, final int picHeightInMbs, final int numSliceGroups) {
        final int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        final int[] groups = new int[picSizeInMbs];

        for (int i = 0; i < picSizeInMbs; i++) {
            final int group = ((i % picWidthInMbs) + (((i / picWidthInMbs) * numSliceGroups) / 2)) % numSliceGroups;
            groups[i] = group;
        }

        return groups;
    }

    /**
     * A foreground macroblock to slice group map. Macroblocks of the last slice
     * group are the background, all the others represent rectangles covering
     * areas with top-left corner specified by topLeftAddr[group] and bottom
     * right corner specified by bottomRightAddr[group].
     *
     * @param numSliceGroups  Total number of slice groups
     * @param topLeftAddr     Addresses of macroblocks that are top-left corners of
     *                        respective slice groups
     * @param bottomRightAddr Addresses macroblocks that are bottom-right corners of
     *                        respective slice groups
     */
    public static int[] buildForegroundMap(final int picWidthInMbs, final int picHeightInMbs, final int numSliceGroups,
                                           final int[] topLeftAddr, final int[] bottomRightAddr) {
        final int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        final int[] groups = new int[picSizeInMbs];

        for (int i = 0; i < picSizeInMbs; i++)
            groups[i] = numSliceGroups - 1;

        for (int iGroup = numSliceGroups - 2; iGroup >= 0; iGroup--) {

            final int yTopLeft = topLeftAddr[iGroup] / picWidthInMbs;
            final int xTopLeft = topLeftAddr[iGroup] % picWidthInMbs;
            final int yBottomRight = bottomRightAddr[iGroup] / picWidthInMbs;
            final int xBottomRight = bottomRightAddr[iGroup] % picWidthInMbs;

            for (int y = yTopLeft; y <= yBottomRight; y++)
                for (int x = xTopLeft; x <= xBottomRight; x++) {
                    final int mbAddr = y * picWidthInMbs + x;
                    groups[mbAddr] = iGroup;
                }
        }

        return groups;
    }

    /**
     * A boxout macroblock to slice group mapping. Only applicable when there's
     * exactly 2 slice groups. Slice group 1 is a background, while slice group
     * 0 is a box in the middle of the frame.
     *
     * @param numberOfMbsInBox number of macroblocks in slice group 0
     */
    public static int[] buildBoxOutMap(final int picWidthInMbs, final int picHeightInMbs, final boolean changeDirection,
                                       final int numberOfMbsInBox) {
        final int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        final int[] groups = new int[picSizeInMbs];
        final int changeDirectionInt = changeDirection ? 1 : 0;

        for (int i = 0; i < picSizeInMbs; i++)
            groups[i] = 1;

        int x = (picWidthInMbs - changeDirectionInt) / 2;
        int y = (picHeightInMbs - changeDirectionInt) / 2;
        int leftBound = x;
        int topBound = y;
        int rightBound = x;
        int bottomBound = y;
        int xDir = changeDirectionInt - 1;
        int yDir = changeDirectionInt;

        boolean mapUnitVacant;
        for (int k = 0; k < numberOfMbsInBox; k += (mapUnitVacant ? 1 : 0)) {
            final int mbAddr = y * picWidthInMbs + x;
            mapUnitVacant = (groups[mbAddr] == 1);
            if (mapUnitVacant) {
                groups[mbAddr] = 0;
            }
            if (xDir == -1 && x == leftBound) {
                leftBound = Math.max(leftBound - 1, 0);
                x = leftBound;
                xDir = 0;
                yDir = 2 * changeDirectionInt - 1;
            } else if (xDir == 1 && x == rightBound) {
                rightBound = Math.min(rightBound + 1, picWidthInMbs - 1);
                x = rightBound;
                xDir = 0;
                yDir = 1 - 2 * changeDirectionInt;
            } else if (yDir == -1 && y == topBound) {
                topBound = Math.max(topBound - 1, 0);
                y = topBound;
                xDir = 1 - 2 * changeDirectionInt;
                yDir = 0;
            } else if (yDir == 1 && y == bottomBound) {
                bottomBound = Math.min(bottomBound + 1, picHeightInMbs - 1);
                y = bottomBound;
                xDir = 2 * changeDirectionInt - 1;
                yDir = 0;
            } else {
                x += xDir;
                y += yDir;
            }
        }

        return groups;
    }

    /**
     * A macroblock to slice group map that fills frame in raster scan.
     */
    public static int[] buildRasterScanMap(final int picWidthInMbs, final int picHeightInMbs, final int sizeOfUpperLeftGroup,
                                           final boolean changeDirection) {
        final int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        final int[] groups = new int[picSizeInMbs];
        final int changeDirectionInt = changeDirection ? 1 : 0;

        int i;
        for (i = 0; i < sizeOfUpperLeftGroup; i++) {
            groups[i] = changeDirectionInt;
        }

        for (; i < picSizeInMbs; i++) {
            groups[i] = 1 - changeDirectionInt;
        }

        return groups;
    }

    /**
     * A macroblock to slice group map that fills frame column by column
     */
    public static int[] buildWipeMap(final int picWidthInMbs, final int picHeightInMbs, final int sizeOfUpperLeftGroup,
                                     final boolean changeDirection) {
        final int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        final int[] groups = new int[picSizeInMbs];
        final int changeDirectionInt = changeDirection ? 1 : 0;

        int k = 0;
        for (int j = 0; j < picWidthInMbs; j++) {
            for (int i = 0; i < picHeightInMbs; i++) {
                final int mbAddr = i * picWidthInMbs + j;
                if (k++ < sizeOfUpperLeftGroup) {
                    groups[mbAddr] = changeDirectionInt;
                } else {
                    groups[mbAddr] = 1 - changeDirectionInt;
                }
            }
        }

        return groups;
    }
}
