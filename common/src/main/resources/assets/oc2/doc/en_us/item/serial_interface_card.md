# Serial Interface Card
![Serial experiments](item:oc2:serial_interface_card)

The serial interface card allows [computers](../block/computer.md) to read and write serial data from a serial port. Serial data is transmitted on a bus established by [network cables](network_cable.md).

A segment is every endpoint connected through [network connectors](../block/network_connector.md) and [network hubs](../block/network_hub.md). It is *multi-drop*: everything one endpoint writes will be readable by every other endpoint. With more than two participants, software needs to filter.

These cards can be configured to only connect to selected sides (use while holding). This allows using multiple cards to build a custom router, for example.

In addition to connectivity, this also allows setting an **address.** This is an arbitrary number in [0, 255) that allows identifying different cards on a bus. Hardware ignores it; software may use it to establish a protocol on top of it. This is purely for convenience.

Baud rate must be configured in software and must match between endpoints on a shared bus. The default after a reset is 9600. Misconfiguration will result in corrupted data. Higher baud rates are more likely to cause collisions unless a protocol prevents this, and weaker systems may not process received data fast enough.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

## Linux

The card is a plain serial port. The first one in a machine is `/dev/ttyS1`, because `ttyS0` is the built-in terminal. Set the rate endpoints on the segment should commonly use:

`stty -F /dev/ttyS1 9600 raw \`
`   -echo -crtscts`
`microcom -s 9600 /dev/ttyS1`

The `oc2.serial` library wraps that, and also reads the address of the card via the HLAPI:

`local serial =`
`  require("oc2.serial")`
`local line = assert(`
`  serial.open(nil, 9600))`
`line:send(7, "status?")`
`local from, message =`
`  line:receive(5000)`

There is also a MicroPython version with the same API.

## CP/M

CP/M uses the first card as its reader and punch, so BDOS functions 3 and 4 go through it.

`STAT CON:=TTY:` switches to the first card as the console, for example to use the computer from a [terminal](../block/terminal.md).  
`STAT CON:=CRT:` goes back to the built-in one.

`SERBAUD` sets another rate from a divisor: 384 is 300 baud, 96 is 1200, 12 is 9600, 6 is 19200, 1 is 115200.

`TERM` is a dumb terminal on the card: everything typed is written, everything read is printed. `Ctrl-]` quits. Corrupted data prints as `~`.

`SERIAL.INC` provides utilities for user programs: finding a card's port as well as IO on it, with status results.

`SERCHAT.Z80` on the system disk is an example program.

## Suggested multi-endpoint framing

The serial port reads and writes raw bytes from and to the bus. To associate data with endpoints, the library expects the following framing:

`01h  to  from  length  payload...  checksum`

The checksum is `to + from + length` plus every payload byte, overflowing. Address 255 means everyone.

## Failure modes

Unlike the [network card](network_interface_card.md), errors are not automatically compensated, so programs must handle them accordingly. The following errors categories exist:

- **Overrun**: the machine is not reading the port fast enough, bytes are dropped.
- **Contention**: received data was corrupted because too many senders flooded the bus at the same time.
- **Collision**: sent data was corrupted because too many senders flooded the bus at the same time.
- **Noise**: traffic arrived at a baud rate this port is not set to.

Error counts can be read via the HLAPI and MLAPI, see below.

## API
Programs can read the card's address, baud rate, port location and error counters through the `serial` device. See the [serial device](../device/serial.md) reference for its methods. **Important**: device order in the HLAPI and MLAPI may differ from the order devices were mounted, i.e. the order they are listed in the device tree/enumeration window.
