/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

/**
 * A MAC address, split so that it fits in two primitives.
 *
 * @param prefix  the leading two octets
 * @param address the trailing four octets
 */
public record MacAddress(short prefix, int address) {
}
