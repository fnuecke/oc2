# VXLAN

The default operating mode of the internet gateway is network address translation (NAT). When enabled in the config, a
second mode can be used, "VXLAN". In this mode the gateway bridges the in-game network to a [Virtual Extensible LAN]
(VXLAN, RFC 7348) tunnel endpoint outside the game. Ethernet frames pass through unchanged, so computers appear on the
outside network as regular hosts. For example, two servers can share one network, or real computers can share a network
with an in-game computer. The latter allows connecting to an in-game computer via SSH, which is neat for development in
a more convenient environment (such as your favorite IDE).

VXLAN mode is **disabled by default**. It has to be enabled in the server's config, and the list of allowed peer
addresses has to be set as well (i.e. where the VXLAN may connect to).

## Security

VXLAN is unauthenticated and unencrypted. Anyone who can reach the bind port and spoof an allowed peer address can
inject frames into in-game networks. Computers can send datagrams to any UDP port on an allowed peer. Only allow peers
on networks you trust, e.g. a private link or a WireGuard tunnel.

None of the NAT mode restrictions apply. The address filter, port filter and bandwidth limits only affect NAT mode.

## Server Configuration

The settings are in the `[admin.vxlan]` section of the server config file. As with all the mods' server settings,
changes only take effect after restarting the world. A valid sample configuration:

```toml
[admin.vxlan]
vxlanEnabled = true
vxlanBindAddress = "0.0.0.0"
vxlanBindPort = 4789
vxlanAllowedPeers = ["192.168.100.2"]
```

All gateways share one socket. Datagrams from addresses that are not in the allowed peer list are dropped. New gateways
use the first allowed peer as their default peer.

Each frame adds 50 bytes of overhead over IPv4, and 70 bytes over IPv6. To carry 1500 byte frames without
fragmentation, the path between the server and the peer needs an MTU of at least 1550 over IPv4, or 1570 over IPv6.
Otherwise, lower the MTU of the guests accordingly, e.g. to 1450 over IPv4.

## Linux Quickstart

This sets up a VXLAN peer on the same Linux machine the server runs on. The peer lives in a network namespace, so the
kernel's VXLAN device does not compete with the server for port 4789. A veth pair connects the namespace to the host.

| Address           | Role                                       |
|-------------------|--------------------------------------------|
| `192.168.100.1`   | Host end of the veth pair, the server.     |
| `192.168.100.2`   | Namespace end of the veth pair, the peer.  |
| `192.168.42.1/24` | The peer's address in the overlay network. |

Both subnets must not overlap a network the host is already on. If they do, pick other private ranges.

The following assumes the `vxlan` kernel module is loaded (`sudo modprobe vxlan`) and that `ethtool` is installed.

Create the namespace and the veth pair:

```sh
sudo ip netns add oc2vx
sudo ip link add oc2vx-host type veth peer name oc2vx-ns
sudo ip link set oc2vx-ns netns oc2vx
sudo ip addr add 192.168.100.1/24 dev oc2vx-host
sudo ip link set oc2vx-host mtu 1600 up
sudo ip -n oc2vx link set lo up
sudo ip -n oc2vx addr add 192.168.100.2/24 dev oc2vx-ns
sudo ip -n oc2vx link set oc2vx-ns mtu 1600 up
```

Create the VXLAN device in the namespace:

```sh
sudo ip -n oc2vx link add vx42 type vxlan id 42 local 192.168.100.2 remote 192.168.100.1 dstport 4789 dev oc2vx-ns
sudo ip -n oc2vx link set vx42 mtu 1500 up
sudo ip -n oc2vx addr add 192.168.42.1/24 dev vx42
sudo ip netns exec oc2vx ethtool -K vx42 tx off
```

The last command disables checksum offload on the VXLAN device. Without it, the kernel leaves UDP and TCP checksums of
frames sent through `vx42` unfinished, since the veth pair never needs them to be finished. The server forwards these
frames into the game as they are, and guests drop them. Traffic through a physical network card doesn't need this.

Configure the server with `vxlanAllowedPeers = ["192.168.100.2"]`. Configure the gateway with peer `192.168.100.2`, port
`4789` and VNI `42`.

### Addresses

To assign addresses to the guests you can run a DHCP server in the namespace. For example, as a temporary setup with
dnsmasq, killable with Ctrl+C:

```sh
sudo ip netns exec oc2vx dnsmasq -d \
  --conf-file=/dev/null --leasefile-ro --port=0 --bind-interfaces \
  --interface=vx42 --dhcp-range=192.168.42.100,192.168.42.200,1h
```

On the in-game computer, request an address with `udhcpc -i eth0` or by using the `setup-network` script, choosing the
DHCP client option.

Alternatively, just set the address on the in-game computer by hand:

```sh
ip addr add 192.168.42.2/24 dev eth0
ip link set eth0 up
```

### Communication

By default, only the namespace has a route to the overlay network. Run commands in it to reach the guests, e.g.:

```sh
sudo ip netns exec oc2vx ping 192.168.42.2
sudo ip netns exec oc2vx sudo -u "$USER" ssh root@192.168.42.2
```

To reach the guests from the host directly, route the overlay network through the namespace:

```sh
sudo ip netns exec oc2vx sysctl -w net.ipv4.ip_forward=1
sudo ip route add 192.168.42.0/24 via 192.168.100.2
```

Guests that got their address from dnsmasq use `192.168.42.1` as their default route, replies work out of the box.
Guests with a manually set address need to be told so via `ip route add default via 192.168.42.1`.

For `ssh`, start the server on the computer with `/etc/init.d/dropbear start`. If you haven't done any user setup and
want to connect as `root`, make sure to set a password first (via `passwd`) to make this work.

### Teardown

Stop dnsmasq, then delete the namespace. This also removes the veth pair, the VXLAN device and the route:

```sh
sudo ip netns del oc2vx
```

[Virtual Extensible LAN]: https://en.wikipedia.org/wiki/VXLAN
