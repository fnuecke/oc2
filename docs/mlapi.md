# Mid-Level API

The mid-level API (MLAPI) is a register page shared by all mid-level API devices on the bus. It is available
on Z80 and RISC-V machines. For a player-facing introduction, see the
[manual](../common/src/main/resources/assets/oc2/doc/en_us/mlapi.md). Function codes and byte layouts of each
device are listed in the [device reference](../common/src/generated/resources/assets/oc2/doc/en_us/device/index.md).

## Access

### Z80

The registers are eight ports, found through the [enumeration window](z80.md#enumeration-window) as class `0x80`
entries.
[ocapi.inc](../common/src/main/resources/data/oc2/cpm/ocapi.inc) provides constants for the registers and the
lookup routine `OCFIND`. It is also on the CP/M boot floppy, and needs `DEVLIB.INC` to be included first.

The interrupt is line 1, vector `0x02`, as reported in `BINT`. A handler should read events until `OCEVP` is cleared,
then returns with `EI` and `RETI`. Use interrupt mode 2. Modes 0 and 1 jump to `0x0038`. `REDWAIT.Z80` on the boot
floppy uses an interrupt to wait for a redstone change.

### RISC-V

The registers are a single page at `0x10000000`, device tree node `oc2,mlapi`, interrupt 6. See
[riscv.md](riscv.md#device-tree).

| Offset | Registers                                                                                                                     |
|--------|-------------------------------------------------------------------------------------------------------------------------------|
| `0x00` | `OCSEL` to `OCEVD`                                                                                                            |
| `0x10` | [Enumeration window](z80.md#enumeration-window), listing itself and the mid-level API devices. Ports are offsets in this page |

Accesses must be 8 bit wide. Wider accesses and accesses to other offsets raise access faults. `BINT` in this window
always reads `0xFF`, use interrupt 6 from the device tree instead.

The stock kernel binds `uio_pdrv_genirq` to the page, so it is available as `/dev/uioN`, where
`/sys/class/uio/uioN/name` reads `oc2-mlapi`. Opening it requires root. Map 4096 bytes at offset 0 of that file. For a
custom kernel, enable `CONFIG_UIO_PDRV_GENIRQ`. The boot argument `uio_pdrv_genirq.of_id=oc2,mlapi` is always set by the
host.

With `OCEVI` set, the interrupt fires when an event is queued. The driver then disables it, until a 32-bit `1` is
written to the file. Reading 4 bytes from the file blocks until the interrupt fired since the last read. Read all queued
events before re-enabling the interrupt, events that are already queued do not fire again. The builtin file system ships
a C header for this, `include/mlapi.h`, and an example, `example/redstone.c`. Its `init.d` adds the header directory to
`C_INCLUDE_PATH`.

## Enumeration

Each mid-level API device has its own entry in the window. All of them report class `0x80` and the same port, the start
of the register page. They differ in:

- Names: the device's name, up to six characters, e.g. `REDSTN`. It is followed by additional names, e.g. label set on
  bus interfaces, see [names](z80.md#enumeration-window). `OCFIND` matches any of them.
- Attributes: the sub-device index. Write it to `OCSEL` to address the device.

The id is a UUID that identifies the device's block face or item, devices on the same one share it. It is the same as
the device's `deviceId` in the [high-level API](hlapi.md#generation). The id survives rescans, reboots and world
reloads. It only changes when devices are added to or removed from that block face or item. Use it to find a device
again after its index changed.

`SYSTEM` is present on every computer and robot, and is always index 0. There are at most 254 devices. Two devices with
the same name provided by the same block face or item are listed only once.

Indices are assigned each time the bus is scanned, so they may change when devices are added or removed. The guest is
not notified of this. A selection made before a rescan keeps addressing the same device. If that device was removed, or
other devices on the same block face or item changed, calls fail with `OCENOD`. Writing an old index to `OCSEL` selects
the possibly new device that this index refers to now. After calls fail with `OCENOD`, look up the device again.

## Registers

| Offset | Name    | Read                                                           | Write                                   |
|--------|---------|----------------------------------------------------------------|-----------------------------------------|
| 0      | `OCSEL` | Selected sub-device                                            | Selects a sub-device, abandons the call |
| 1      | `OCFUN` | `0`                                                            | Function code, begins a call            |
| 2      | `OCDAT` | Next result byte, or the error code while the error bit is set | Next argument byte                      |
| 3      | `OCSTA` | Status                                                         | `0x00` aborts, any other value executes |
| 4      | `OCEVC` | Event status                                                   | Event control                           |
| 5      | `OCEVD` | Next event byte, `0xFF` if none is queued                      | -                                       |
| 6-7    |         | `0`                                                            | -                                       |

Status bits: `0x01` busy, `0x02` result byte available, `0x80` error.

Error codes: `0x01` no such sub-device, `0x02` no such function, `0x05` invalid arguments, `0x06` internal device error.
Writing `OCSEL` or `OCFUN`, and aborting while not busy, clear the error.

- Writing `OCDAT` before `OCFUN` or after executing sets invalid arguments error.
- Writing a 257th argument byte sets invalid arguments and abandons the call. Executing it after that fails with no such
  function.
- Executing without writing `OCFUN` first fails with no such function. Every call needs its own `OCFUN` write.
- Too few argument bytes, or not enough energy for the call, fail with invalid arguments. Extra argument bytes are
  ignored.
- A device producing more than 256 result bytes fails with internal error.
- `OCDAT` reads `0` once all results have been read.

## Call Sequence

1. Write the index to `OCSEL`.
2. Write the function code to `OCFUN`. Codes are `0x00`-`0xFE`, `0xFF` fails with no such function.
3. Write the argument bytes to `OCDAT`, at most 256. Multi-byte values are little-endian.
4. Write `0x01` to `OCSTA`.
5. Poll `OCSTA` until busy is cleared. Read `OCDAT` while the available bit is set, at most 256 bytes.

Most functions complete immediately. Functions that interact with the world run in the next server tick, busy stays set
until then. If the device was removed in the meantime, the call fails with no such sub-device.

While busy, writes to `OCSEL`, `OCFUN`, `OCDAT` and `OCSTA` are ignored. Writing `0x00` to `OCSTA` abandons the call,
but does not cancel it. It still runs and has its effect, busy stays set until it completes, and its result is
discarded.

There is one selection and one call per machine. Do not use registers 0-3 from an interrupt handler, while the main
program may be using them.

## Events

Devices can queue events, for example the redstone interface when a received signal changes. Event codes and values of
each device are listed in the device reference.

`OCEVC` bits:

| Bit    | Name    | Meaning                                                                               |
|--------|---------|---------------------------------------------------------------------------------------|
| `0x01` | `OCEVQ` | Read/write. Queue events. Clearing it discards queued events                          |
| `0x02` | `OCEVI` | Read/write. Raise the interrupt while events are queued                               |
| `0x40` | `OCEVO` | Read. Events were dropped, because the queue was full. Any write to `OCEVC` clears it |
| `0x80` | `OCEVP` | Read. An event is queued                                                              |

Both enable bits are cleared after a reset, and events are dropped until `OCEVQ` is set. The queue holds 16 events. When
it is full, new events are dropped and `OCEVO` is set. To get back in sync, read the current state using calls.

An event is four bytes, read from `OCEVD` in this order: sub-device index, event code, value low byte, value high byte.
Reading the fourth byte removes the event. The index is the device's index at the time the event is read. Events of
devices that are removed from the bus are discarded.

All mid-level API devices share one interrupt. On the Z80, it is level-triggered, and raised while `OCEVI` is set and
events are queued. On RISC-V, while `OCEVI` is set, it is raised when an event is queued, when an event was read and
more remain, and when `OCEVC` is written or the bus was rescanned, if events are queued.

Registers 4 and 5 are independent of calls, including while busy. This means a handler may use them while the main
program is in the middle of a call.
