/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import javax.annotation.Nullable;

public interface InternetAdapter {
    @Nullable
    byte[] readInternetFrame();

    void writeInternetFrame(byte[] frame);
}
