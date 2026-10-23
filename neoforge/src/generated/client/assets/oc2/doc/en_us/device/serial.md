# Serial

## High-level API
Device name: `serial`

Provided by the [serial interface card](../item/serial_interface_card.md).

The serial port itself is driven through the operating system, see the card's page. This device reports the card's configuration and error counters, for example to tell serial ports apart or to detect collisions.

Counters reset when the computer starts. Device order here may differ from the order the cards were installed in, so match them up by `getBaseAddress()`.

### Methods

`getAddress():number`
Gets the address this card is set to. The hardware ignores it; software may use it to address endpoints on a shared segment.
- Returns the address, in [0, 255].

`getBaseAddress():number`
Gets where this card's serial port registers are mapped, to tell which port belongs to which card when there is more than one.
- Returns the memory address on RISC-V, the I/O port on the Z80.

`getBaudRate():number`
Gets the baud rate the serial port is configured to. Every endpoint on a segment must be set to the same rate.
- Returns the baud rate in bits per second, or `0` while the computer is off.

`getNoiseCount():number`
Gets how often traffic arrived at a baud rate this port is not set to, including this computer's own port being set to a different rate.
- Returns the number of data errors since the computer started.

`getOverrunCount():number`
Gets how many received bytes were lost because this computer did not read them in time.
- Returns the number of bytes lost since the computer started.

`getRxErrorCount():number`
Gets how often received data was corrupted because several endpoints sent at the same time. These are the collisions the receiver sees.
- Returns the number of receiver collisions since the computer started.

`getTxErrorCount():number`
Gets how often sent data was corrupted because several endpoints sent at the same time. These are the collisions the sender sees. Read it before sending and again once the port finished sending: a change means the send failed.
- Returns the number of sender collisions since the computer started.

## Mid-level API
Device name: `SERIAL`

Counters are clamped to two bytes. Values wider than a byte are low byte first.

Device order here may differ from the order the cards were installed in, so match them up by `getBaseAddress`.

### Methods

`1 getAddress`
Reads the address this card is set to.
- Returns one byte, the address.

`2 getBaudRate`
Reads the baud rate the serial port is configured to.
- Returns four bytes, the baud rate in bits per second.

`3 getOverrunCount`
Reads how many received bytes were lost because this computer did not read them in time.
- Returns two bytes, the count.

`4 getRxErrorCount`
Reads how often received data was corrupted by other endpoints sending at the same time.
- Returns two bytes, the count.

`5 getTxErrorCount`
Reads how often sent data was corrupted by other endpoints sending at the same time. A change after a send means it failed.
- Returns two bytes, the count.

`6 getNoiseCount`
Reads how often traffic arrived at a baud rate this port is not set to.
- Returns two bytes, the count.

`7 getBaseAddress`
Reads the I/O port this card's serial port registers start at, to tell cards apart.
- Returns one byte, the port.
