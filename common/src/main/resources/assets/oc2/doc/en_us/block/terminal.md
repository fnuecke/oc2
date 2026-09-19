# Terminal
![Remote viewing](block:oc2:terminal)

A terminal is a standalone screen and keyboard without the rest of the computer, connected to a serial bus using a [network connector](network_connector.md). It reads and writes serial data verbatim. Use a [serial card](../item/serial_interface_card.md) in a computer to enable interaction with the computer.

On Linux, use `getty` to spawn a session on the terminal:  
`setsid getty -L ttyS1 9600 screen &`

On CP/M, switch to an external terminal like so:  
`STAT CON:=TTY:`.

## Configuration
Configure the terminal address and baud rate in its settings screen. To access this screen, either open the terminal with a [wrench](../item/wrench.md), or switch to the settings screen using the toggle button on the left of the terminal screen.
