# Serial Ports

On the Z80, these are accessible as `UART` devices (class `0x02`) on
the [enumeration window](z80.md#enumeration-window). On RISC-V as `ns16550a` [device tree nodes](riscv.md#mod-devices).

The device emulates the 16550A register layout of eight ports. On the Z80, the first class `0x02` entry is the console,
connected to the computer's terminal. Further entries are generally serial interface cards, which are documented
in [serial_interface_card.md](../common/src/main/resources/assets/oc2/doc/en_us/item/serial_interface_card.md) together
with the helper routines in [serial.inc](https://github.com/fnuecke/sedna-cpm/blob/main/src/main/asm/serial.inc).

The implementation of the emulated device is based
on [this reference](https://web.archive.org/web/20200207194832/https://www.lammertbies.nl/comm/info/serial-uart).

| Offset | Read                                                     | Write                               |
|--------|----------------------------------------------------------|-------------------------------------|
| 0      | Receive buffer; divisor low byte when `LCR` bit 7 is set | Transmit holding; divisor low byte  |
| 1      | Interrupt enable; divisor high byte                      | Interrupt enable; divisor high byte |
| 2      | Interrupt identification                                 | FIFO control                        |
| 3      | Line control                                             | Line control                        |
| 4      | Modem control                                            | Modem control                       |
| 5      | Line status                                              | -                                   |
| 6      | Modem status                                             | -                                   |
| 7      | Scratch                                                  | Scratch                             |

Registers behave as described in the reference, with these differences:

- Line status never reports parity errors (`0x04`) or FIFO errors (`0x80`).
- The divisor resets to 12, i.e. 9600 baud.
- The console ignores the baud rate. On serial interface cards, it sets the throughput, and mismatched rates cause
  framing errors.
- On the Z80, the console's interrupt is not wired, poll its line status register instead.
