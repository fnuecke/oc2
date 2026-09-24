# OpenComputers II Serial Interface

An OpenComputers II network connector placed on a serial port module acts as a serial interface. The connected-to segment is a bus. Every other connected device - computers, terminals - may read sent data, and send back data.

Each value written is sent as one byte, its low eight bits. Each byte received is read as one value in the range [0, 255]. A received byte that was corrupted on the line is read with the high byte non-zero. Corruption may occur when several endpoints sent at the same time or one of them uses a different baud rate.

The interface sends and receives at `300` baud. It buffers only a small amount of received data, so reading should happen as quickly as possible.
