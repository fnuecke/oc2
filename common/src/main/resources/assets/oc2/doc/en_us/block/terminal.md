# Terminal
![Remote viewing](block:oc2:terminal)

A terminal is a standalone screen and keyboard without the rest of the computer, connected to a serial bus using a [network connector](network_connector.md). It reads and writes serial data verbatim. Use a [serial card](../item/serial_interface_card.md) in a computer to enable interaction with the computer.

# OpenOS II
The console can be moved to a terminal:  
`console /dev/ttyS1`  
The shell then runs on the terminal, and the computer's own screen stays unchanged. It can be moved back using `console /dev/ttyS0`. To do this automatically at boot time, in `/init.d/50-console.lua`:  
`os.execute("console /dev/ttyS1")`  

The default on both ends is 9600 baud. For faster output, set the same rate in the terminal's settings and with `stty -F /dev/ttyS1 115200`.

# Linux
Use `getty` to spawn a session on the terminal:  
`setsid getty -L ttyS1 9600 screen &`

# CP/M
Switch to an external terminal like so:  
`STAT CON:=TTY:`

## Configuration
Configure the terminal address and baud rate in its settings screen. To access this screen, either open the terminal with a [wrench](../item/wrench.md), or switch to the settings screen using the toggle button on the left of the terminal screen.
