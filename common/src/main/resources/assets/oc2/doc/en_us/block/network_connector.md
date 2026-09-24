# Network Connector
![The data must flow](block:oc2:network_connector)

The network connector is a fundamental part for building a network connecting different [computers](computer.md). Use [network cables](../item/network_cable.md) to connect connectors with each other.

Each connector will represent one "hop" for relayed packets. Packets may only travel a limited number of hops before they are dropped.

Note that each connector only supports up to two attached network cables. Chain multiple connectors to increase the range that can be spanned.

Create a simple multi-computer network by chaining the connectors attached to the computers. To create a mesh network, use a [network hub](network_hub.md). The hub connects all attached connectors with each other.

## Third-Party Computers
Connectors may be used to enable serial communication with computers manufactured by other vendors. When attached to a third-party computer, the connector will provide an endpoint that allows sending and receiving serial data.

### ComputerCraft
To connect to computers of the ComputerCraft brand, connectors may be mounted on computers and will be recognized as a peripheral of type `oc2_serial_interface`. Sends data using `write(data)`; it which returns how many bytes fit into the send buffer. Read data using `read([count])`; it returns the received bytes along with how many of them were corrupted, or nothing when there was no data.

`available()` allows querying how much data is ready, `clear()` drops the receive buffer.

When new data arrives, the event `oc2_serial_data` is queued with the peripheral's name. Use this to avoid polling.

The baud rate and address are read and set with `getBaudRate()`, `setBaudRate(rate)` for rates from 300 to 115200, `getAddress()` and `setAddress(address)` for addresses from 0 to 254, and the error counters of the [serial device](../device/serial.md) are available as well. After a reload the peripheral is reset to 9600 baud with a fresh address. Set them up on program start.

`local serial = peripheral.find(`
`  "oc2_serial_interface")`
`serial.write("hello!\n")`
`os.pullEvent("oc2_serial_data")`
`print(serial.read())`

### TIS-3D
For the TIS-3D computer, a connector mounted on a serial port module will enable data exchange. Each value written is sent as one byte, each byte received is read as one value, with the high byte non-zero when the byte was corrupted on the line. For further details, see the protocol's page in the TIS-3D manual.
