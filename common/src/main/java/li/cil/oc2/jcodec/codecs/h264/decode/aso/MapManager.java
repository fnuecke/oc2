/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode.aso;

import li.cil.oc2.jcodec.codecs.h264.io.model.PictureParameterSet;
import li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class MapManager {
    private final SeqParameterSet sps;
    private final PictureParameterSet pps;
    private MBToSliceGroupMap mbToSliceGroupMap;
    private int prevSliceGroupChangeCycle;

    public MapManager(final SeqParameterSet sps, final PictureParameterSet pps) {
        this.sps = sps;
        this.pps = pps;
        this.mbToSliceGroupMap = buildMap(sps, pps);
    }

    private MBToSliceGroupMap buildMap(final SeqParameterSet sps, final PictureParameterSet pps) {
        final int numGroups = pps.numSliceGroupsMinus1 + 1;

        if (numGroups > 1) {
            final int[] map;
            final int picWidthInMbs = sps.picWidthInMbsMinus1 + 1;
            final int picHeightInMbs = SeqParameterSet.getPicHeightInMbs(sps);

            if (pps.sliceGroupMapType == 0) {
                final int[] runLength = new int[numGroups];
                for (int i = 0; i < numGroups; i++) {
                    runLength[i] = pps.runLengthMinus1[i] + 1;
                }
                map = SliceGroupMapBuilder.buildInterleavedMap(picWidthInMbs, picHeightInMbs, runLength);
            } else if (pps.sliceGroupMapType == 1) {
                map = SliceGroupMapBuilder.buildDispersedMap(picWidthInMbs, picHeightInMbs, numGroups);
            } else if (pps.sliceGroupMapType == 2) {
                map = SliceGroupMapBuilder.buildForegroundMap(picWidthInMbs, picHeightInMbs, numGroups, pps.topLeft,
                    pps.bottomRight);
            } else if (pps.sliceGroupMapType >= 3 && pps.sliceGroupMapType <= 5) {
                return null;
            } else if (pps.sliceGroupMapType == 6) {
                map = pps.sliceGroupId;
            } else {
                throw new RuntimeException("Unsupported slice group map type");
            }

            return buildMapIndices(map, numGroups);
        }

        return null;
    }

    private MBToSliceGroupMap buildMapIndices(final int[] map, final int numGroups) {
        int[] ind = new int[numGroups];
        final int[] indices = new int[map.length];

        for (int i = 0; i < map.length; i++) {
            indices[i] = ind[map[i]]++;
        }

        final int[][] inverse = new int[numGroups][];
        for (int i = 0; i < numGroups; i++) {
            inverse[i] = new int[ind[i]];
        }
        ind = new int[numGroups];
        for (int i = 0; i < map.length; i++) {
            final int sliceGroup = map[i];
            inverse[sliceGroup][ind[sliceGroup]++] = i;
        }

        return new MBToSliceGroupMap(map, indices, inverse);
    }

    private void updateMap(final SliceHeader sh) {
        final int mapType = pps.sliceGroupMapType;
        final int numGroups = pps.numSliceGroupsMinus1 + 1;

        if (numGroups > 1 && mapType >= 3 && mapType <= 5
            && (sh.sliceGroupChangeCycle != prevSliceGroupChangeCycle || mbToSliceGroupMap == null)) {

            prevSliceGroupChangeCycle = sh.sliceGroupChangeCycle;

            final int picWidthInMbs = sps.picWidthInMbsMinus1 + 1;
            final int picHeightInMbs = SeqParameterSet.getPicHeightInMbs(sps);
            final int picSizeInMapUnits = picWidthInMbs * picHeightInMbs;
            int mapUnitsInSliceGroup0 = sh.sliceGroupChangeCycle * (pps.sliceGroupChangeRateMinus1 + 1);
            mapUnitsInSliceGroup0 = Math.min(mapUnitsInSliceGroup0, picSizeInMapUnits);

            final int sizeOfUpperLeftGroup = pps.sliceGroupChangeDirectionFlag
                ? picSizeInMapUnits - mapUnitsInSliceGroup0
                : mapUnitsInSliceGroup0;

            final int[] map;
            if (mapType == 3) {
                map = SliceGroupMapBuilder.buildBoxOutMap(picWidthInMbs, picHeightInMbs,
                    pps.sliceGroupChangeDirectionFlag, mapUnitsInSliceGroup0);
            } else if (mapType == 4) {
                map = SliceGroupMapBuilder.buildRasterScanMap(picWidthInMbs, picHeightInMbs, sizeOfUpperLeftGroup,
                    pps.sliceGroupChangeDirectionFlag);
            } else {
                map = SliceGroupMapBuilder.buildWipeMap(picWidthInMbs, picHeightInMbs, sizeOfUpperLeftGroup,
                    pps.sliceGroupChangeDirectionFlag);
            }

            this.mbToSliceGroupMap = buildMapIndices(map, numGroups);
        }
    }

    public Mapper getMapper(final SliceHeader sh) {
        updateMap(sh);
        final int firstMBInSlice = sh.firstMbInSlice;
        if (pps.numSliceGroupsMinus1 > 0) {

            return new PrebuiltMBlockMapper(mbToSliceGroupMap, firstMBInSlice, sps.picWidthInMbsMinus1 + 1);
        } else {
            return new FlatMBlockMapper(sps.picWidthInMbsMinus1 + 1, firstMBInSlice);
        }
    }
}
