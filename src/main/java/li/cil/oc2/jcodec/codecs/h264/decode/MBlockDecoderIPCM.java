/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.common.model.Picture;

import static li.cil.oc2.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static li.cil.oc2.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

/**
 * A decoder for Intra PCM macroblocks
 *
 * @author The JCodec project
 */
public final class MBlockDecoderIPCM {
    private final Mapper mapper;
    private final DecoderState s;

    public MBlockDecoderIPCM(final Mapper mapper, final DecoderState decoderState) {
        this.mapper = mapper;
        this.s = decoderState;
    }

    public void decode(final MBlock mBlock, final Picture mb) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        collectPredictors(s, mb, mbX);
        saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }
}
