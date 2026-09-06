/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import li.cil.oc2.common.inet.InetUtils;
public sealed interface SessionKey {
    int destinationIpAddress();

    record Echo(int sourceIpAddress, int destinationIpAddress, short identity) implements SessionKey {
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("icmp ");
            InetUtils.ipv4AddressToString(builder, sourceIpAddress);
            builder.append(" -> ");
            InetUtils.ipv4AddressToString(builder, destinationIpAddress);
            return builder.append(" id=").append(Short.toUnsignedInt(identity)).toString();
        }
    }

    record Datagram(int sourceIpAddress, short sourcePort, int destinationIpAddress,
                    short destinationPort) implements SessionKey {
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("udp ");
            InetUtils.socketAddressToString(builder, sourceIpAddress, sourcePort);
            builder.append(" -> ");
            InetUtils.socketAddressToString(builder, destinationIpAddress, destinationPort);
            return builder.toString();
        }
    }

    record Stream(int sourceIpAddress, short sourcePort, int destinationIpAddress,
                  short destinationPort) implements SessionKey {
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("tcp ");
            InetUtils.socketAddressToString(builder, sourceIpAddress, sourcePort);
            builder.append(" -> ");
            InetUtils.socketAddressToString(builder, destinationIpAddress, destinationPort);
            return builder.toString();
        }
    }
}
