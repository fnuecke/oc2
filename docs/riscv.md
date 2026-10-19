# RISC-V Machine

This document describes the hardware a RISC-V computer presents to the software running on it. It aims to enable writing
a custom firmware and OS. The emulator is [Sedna](https://github.com/fnuecke/sedna). The stock firmware and Linux image
are built from the `sedna` branch of [fnuecke/buildroot](https://github.com/fnuecke/buildroot/tree/sedna). See
its [defconfig](https://github.com/fnuecke/buildroot/blob/sedna/configs/sedna-riscv64_defconfig) as a reference for
building a Linux image.

## Overview

- Single hart, id 0. Little-endian.
- ISA `rv64imafdcsu`, with Zicsr and Zifencei. Machine, supervisor and user mode. `misa` is read-only.
- `satp` accepts Bare, Sv39 and Sv48. Writes with other modes are ignored. ASIDs are not supported, the ASID field reads
    0. The device tree advertises `riscv,sv48`.
- No PMP, no hardware performance counters, no Sstc. `mvendorid`, `marchid` and `mimpid` read 0.
- Misaligned loads and stores are handled in hardware. Misaligned instruction fetches trap.
- `mtime` and the `time` CSR count emulated CPU cycles, so they fall behind wall time when the server lags.
  They start at 0 on every boot. `time` is readable from every mode. `timebase-frequency` equals
  `clock-frequency`, the server setting `vm.riscvCycleBudgetPerSecond` (25 MHz by default).

## Reset and Boot

A machine without a flash memory item does not start. RAM is zeroed on every start. Before the first instruction, the
host writes the device tree blob into RAM, and copies the contents of the flash memory item to `0x80000000`. The hart
starts at `0x1000` in machine mode, with all integer and floating-point registers, `fcsr`, `mtvec`, `mie` and `satp`
zero, `mstatus.FS` off, and interrupts disabled. The PLIC and CLINT are reset as well.

`0x1000` is a 256-byte read-only boot ROM with this trampoline:

```
0x1000  auipc t0, 0
0x1004  ld    a1, 0x10(t0)
0x1008  ld    t0, 0x18(t0)
0x100c  jr    t0
0x1010  .dword <device tree address>
0x1018  .dword 0x80000000
```

Register state when entering the firmware:

| Register   | Value                                    |
|------------|------------------------------------------|
| `pc`, `t0` | `0x80000000`                             |
| `a0`       | 0, the hart id                           |
| `a1`       | Physical address of the device tree blob |
| Others     | 0                                        |

The flash memory item itself is not mapped. Firmware runs from RAM, and may overwrite itself.

## System Controller

The system controller (`syscon`) has these 32-bit registers:

| Offset | Read                                            | Write                                 |
|--------|-------------------------------------------------|---------------------------------------|
| `0x00` | `0`                                             | `0x1000` reboots, `0x2000` powers off |
| `0x04` | `0x53534331` (`"SSC1"`)                         | -                                     |
| `0x08` | Control                                         | Control. Bit 1 enables requests       |
| `0x0C` | Pending requests. Bit 0 power off, bit 1 reboot | Writing 1 clears the bit              |

Powering off stops the machine. Rebooting stops it and starts it again: devices are remounted, the device tree
is rebuilt and the flash memory is copied again.

When a player turns off or restarts the computer, the host stops it directly. If the guest set the enable bit,
the host instead sets the matching request bit and raises interrupt 7. This allows the guest to shut down
cleanly. The interrupt stays raised until all request bits are cleared. A forced power off always stops the machine
directly. The control register is cleared on reset. The stock kernel's `sedna-syscon` driver handles this.

## Memory Map

| Address                   | Size        | Device                                    | `compatible`             |
|---------------------------|-------------|-------------------------------------------|--------------------------|
| `0x00001000`              | `0x100`     | Boot ROM                                  | `mtd-rom`                |
| `0x01000000`              | `0x10`      | System controller                         | `sedna,syscon`, `syscon` |
| `0x02000000`              | `0xC0000`   | Core-local interruptor                    | `riscv,clint0`           |
| `0x0C000000`              | `0x4000000` | Platform interrupt controller, 31 sources | `riscv,plic0`            |
| `0x10000000`              | `0x1000`    | [Mid-level API](mlapi.md#risc-v)          | `oc2,mlapi`              |
| `0x10000000`-`0x7FFFFFFF` |             | Device range                              |                          |
| `0x80000000`-`0xFFFFFFFF` |             | RAM range                                 |                          |

The PLIC's size is that of the standard PLIC layout, which has room for 15872 contexts. With two contexts,
only the first `0x202000` bytes are in use.

All other devices are allocated first-fit from a start address, when the machine starts. RAM modules are placed from
`0x80000000`, built-in devices from `0x10000000` after the mid-level API, devices in the computer's own slots from
`0x20000000 + slot × 0x1000` and devices attached to the bus from `0x30000000`. Addresses may differ between boots, and
memory and flash sizes are variable (server config). Always read addresses, sizes and interrupt numbers from the device
tree.

## Device Tree

The blob (version 17) is built at every start. It is placed at the end of the highest RAM module, 8-byte aligned. Its
address is passed in `a1`.

This is the tree of a computer with one 8 MiB memory module and a hard drive in its first slot. Addresses of
devices outside `/soc`, and interrupt numbers of mod devices, depend on the installed hardware.

```dts
/ {
    #address-cells = <2>;
    #size-cells = <2>;
    compatible = "riscv-sedna", "riscv-virtio";
    model = "riscv-virtio,sedna";

    cpus {
        #address-cells = <1>;
        #size-cells = <0>;
        timebase-frequency = <25000000>;
        cpu-map {
            cluster0 {
                core0 = <&cpu0>;
            };
        };
        cpu0: cpu@0 {
            device_type = "cpu";
            reg = <0>;
            status = "okay";
            compatible = "riscv";
            riscv,isa = "rv64imafdcsu";
            mmu-type = "riscv,sv48";
            clock-frequency = <25000000>;
            cpu0_intc: interrupt-controller {
                #interrupt-cells = <1>;
                interrupt-controller;
                compatible = "riscv,cpu-intc";
            };
        };
    };

    soc {
        #address-cells = <2>;
        #size-cells = <2>;
        compatible = "simple-bus";
        ranges;

        flash@1000 {
            reg = <0x0 0x1000 0x0 0x100>;
            compatible = "mtd-rom";
            bank-width = <4>;
        };
        syscon: syscon@1000000 {
            reg = <0x0 0x1000000 0x0 0x10>;
            interrupts-extended = <&plic 7>;
            compatible = "sedna,syscon", "syscon";
        };
        reboot {
            compatible = "syscon-reboot";
            regmap = <&syscon>;
            offset = <0x0>;
            value = <0x1000>;
        };
        poweroff {
            compatible = "syscon-poweroff";
            regmap = <&syscon>;
            offset = <0x0>;
            value = <0x2000>;
        };
        clint@2000000 {
            reg = <0x0 0x2000000 0x0 0xc0000>;
            interrupts-extended = <&cpu0_intc 3>, <&cpu0_intc 7>;
            compatible = "riscv,clint0";
        };
        plic: plic@c000000 {
            reg = <0x0 0xc000000 0x0 0x4000000>;
            interrupts-extended = <&cpu0_intc 11>, <&cpu0_intc 9>;  // context 0: M-mode, context 1: S-mode
            #address-cells = <0>;
            #interrupt-cells = <1>;
            interrupt-controller;
            compatible = "riscv,plic0";
            riscv,ndev = <31>;
        };
        rtc@10001000 {  // host time
            reg = <0x0 0x10001000 0x0 0x20>;
            interrupts-extended = <&plic 1>;
            compatible = "google,goldfish-rtc";
        };
        rtc@10001020 {  // world time
            reg = <0x0 0x10001020 0x0 0x20>;
            interrupts-extended = <&plic 2>;
            compatible = "google,goldfish-rtc";
        };
    };

    oc2-mlapi@10000000 {
        reg = <0x0 0x10000000 0x0 0x1000>;
        interrupts-extended = <&plic 6>;
        compatible = "oc2,mlapi";
    };
    virtio@10001040 {  // console, high-level API ports
        reg = <0x0 0x10001040 0x0 0x108>;
        interrupts-extended = <&plic 3>;
        compatible = "virtio,mmio";
    };
    uart@10001148 {  // terminal
        reg = <0x0 0x10001148 0x0 0x100>;
        interrupts-extended = <&plic 4>;
        compatible = "ns16550a";
        clock-frequency = <1843200>;
    };
    virtio@10001248 {  // 9P, tag "builtin"
        reg = <0x0 0x10001248 0x0 0x109>;
        interrupts-extended = <&plic 5>;
        compatible = "virtio,mmio";
    };
    virtio@20000000 {  // hard drive in slot 0
        reg = <0x0 0x20000000 0x0 0x138>;
        interrupts-extended = <&plic 31>;
        compatible = "virtio,mmio";
    };
    memory@80000000 {
        reg = <0x0 0x80000000 0x0 0x800000>;
        device_type = "memory";
    };

    chosen {
        stdout-path = "/uart@10001148";
        bootargs = "root=/dev/vda rw uio_pdrv_genirq.of_id=oc2,mlapi";
    };
    reserved-memory {
        #address-cells = <2>;
        #size-cells = <2>;
        ranges;
        firmware@80000000 {
            reg = <0x0 0x80000000 0x0 0x2000>;
        };
    };
};
```

There is one `memory` node per memory module. `bootargs` is always the same. The reserved firmware region is
8 KiB, regardless of the flash memory's capacity. Mod devices other than memory are direct children of `/`,
named `virtio@...`, `uart@...` or `framebuffer@...`.

Use these access widths: UART 8 bit, PLIC 32 bit, CLINT and RTC 32 or 64 bit, mid-level API 8 bit. For virtio,
the registers below offset `0x100` are 32 bit, the configuration space above accepts 8, 16 and 32 bit.
Accesses to unmapped addresses raise access faults, as do wider accesses to the mid-level API. Other devices do
not check the width. For example, reading the PLIC with a width other than 32 bit returns 0.

### Built-in Devices

These are present in every machine. Their interrupt numbers are fixed.

| Device            | `compatible`          | IRQ | Notes                                                                                                                          |
|-------------------|-----------------------|-----|--------------------------------------------------------------------------------------------------------------------------------|
| Real-time clock   | `google,goldfish-rtc` | 1   | Host wall-clock time, in whole seconds.                                                                                        |
| Real-time clock   | `google,goldfish-rtc` | 2   | World time: total ticks since world creation, 3.6 s per tick, starting at the Unix epoch.                                      |
| Virtio console    | `virtio,mmio`         | 3   | Multiport. The ports `oc2.rpc.0`, `oc2.blob.0` and `oc2.event.0` provide the [high-level API](hlapi.md). Look them up by name. |
| Console UART      | `ns16550a`            | 4   | The terminal. `stdout-path`.                                                                                                   |
| Virtio 9P         | `virtio,mmio`         | 5   | Mount tag `builtin`. Contents come from data packs.                                                                            |
| Mid-level API     | `oc2,mlapi`           | 6   | See [mlapi.md](mlapi.md#risc-v).                                                                                               |
| System controller | `sedna,syscon`        | 7   | See [System Controller](#system-controller).                                                                                   |

The real-time clocks' interrupts never fire, they're just there to make the Linux drivers accept the devices.

### Mod Devices

| Item or block                                          | `compatible`         | Virtio device ID |
|--------------------------------------------------------|----------------------|------------------|
| Memory                                                 | `memory` node        |                  |
| Hard drive, disk drive, flash drive                    | `virtio,mmio`        | 2, block         |
| Network interface card, network tunnel card and module | `virtio,mmio`        | 1, network       |
| Keyboard                                               | `virtio,mmio`        | 18, input        |
| Sound card                                             | `virtio,mmio`        | 25, sound        |
| Projector                                              | `simple-framebuffer` |                  |
| Serial interface card                                  | `ns16550a`           |                  |
| Flash memory                                           | none, copied to RAM  |                  |

All virtio devices use MMIO transport version 2, and share the same `compatible` string. Use the device ID register to
check the actual device type. A disk drive or flash drive without media is still present, with a capacity of 0. The
projector framebuffer is 640×480, `r5g6b5`, stride 1280, in the device range. Interrupts of mod devices are assigned
from 31 downwards.

For references, see: [virtio 1.2](https://docs.oasis-open.org/virtio/virtio/v1.2/virtio-v1.2.html), Linux bindings
for [simple-framebuffer](https://github.com/torvalds/linux/blob/master/Documentation/devicetree/bindings/display/simple-framebuffer.yaml),
[syscon-reboot](https://github.com/torvalds/linux/blob/master/Documentation/devicetree/bindings/power/reset/syscon-reboot.yaml),
[syscon-poweroff](https://github.com/torvalds/linux/blob/master/Documentation/devicetree/bindings/power/reset/syscon-poweroff.yaml),
[8250](https://github.com/torvalds/linux/blob/master/Documentation/devicetree/bindings/serial/8250.yaml),
and the [goldfish RTC driver](https://github.com/torvalds/linux/blob/master/drivers/rtc/rtc-goldfish.c).

## Stock Firmware

[`sedna-fw`](https://github.com/fnuecke/buildroot/tree/sedna/boot/sedna-fw/src), shipped as
`oc2:block_devices/flash/riscv.bin`. It runs in machine mode, linked at `0x80000000`. Code, data and its 2 KiB stack fit
into the 8 KiB reserved region. It uses the memory 1 MiB above the RAM base as scratch space while loading.

### Boot

1. Reads the device tree: the first `memory` node, the first `ns16550a` for messages, the CLINT, and up to 8
   `virtio,mmio` nodes with device ID 2, in tree order. Tree order is ascending address, so drives in the computer's
   slots come before drives on the bus.
2. Tries each block device in turn, and boots the first that passes the checks in [Disk Images](#disk-images).
3. If none does, prints `sedna-fw: no bootable disk` once, waits 10,000,000 timer ticks (0.4 s at the default clock) and
   tries again. This way media inserted into a drive listed in the device tree later are still picked up.

When entering the kernel, the hart is in supervisor mode, `pc` points to the loaded image, `a0 = 0`, `a1` is the
device tree address, `satp = 0` and `mie = 0`. Other registers contain whatever the firmware left there.
`medeleg = 0xB1FF` delegates all standard exceptions except environment calls from S and M mode. `mideleg` delegates
supervisor software, timer and external interrupts. `mcounteren` is `0x7`. The block device used is reset before
entering the kernel.

### SBI

Specification version 1.0, implementation ID `0x53`, implementation version 1.

| Extension | EID          | Functions                                                                                |
|-----------|--------------|------------------------------------------------------------------------------------------|
| Base      | `0x10`       | All                                                                                      |
| Timer     | `0x54494D45` | `set_timer`. The machine timer interrupt is forwarded as `STIP`.                         |
| RFENCE    | `0x52464E43` | `remote_fence_i`, `remote_sfence_vma`, `remote_sfence_vma_asid`. Hart masks are ignored. |

All other calls, including the legacy v0.1 extensions, return `SBI_ERR_NOT_SUPPORTED`. The System Reset extension is not
implemented, use the `syscon-poweroff` and `syscon-reboot` nodes instead. An unexpected trap into machine mode prints
`mcause` and `mepc` on the console UART, and halts.

## Images

### Flash Images

Raw binary data, copied to `0x80000000` and entered at its first byte in machine mode, with the state described above.
An image must fit the flash memory item's capacity, 8 KiB for the standard item. Can be any program, the machine does
not require Linux.

The device tree is at the end of RAM, and is not reserved. Firmware must read or move it before using that memory.

### Disk Images

To boot from a block device using the stock firmware, it has satisfy the follow following requirements:

- An ext2 file system starting at byte 0 of the device. No partition table.
- Superblock revision 0 or 1, block size 1, 2 or 4 KiB.
- Classic block maps, and 32-byte group descriptors **directly after** the superblock. The firmware doesn't check
  feature flags, file systems using them won't be bootable.
- A file of at least 60 bytes at `/boot/Image`, addressed by direct, indirect or double-indirect blocks.
  Neither `/boot` nor `Image` may be symbolic links.
- A RISC-V
  Linux [image header](https://github.com/torvalds/linux/blob/master/Documentation/arch/riscv/boot-image-header.rst):
  `text_offset`, as a 64-bit value at byte 8, must be `0x200000`, and the magic `RSC\x05` must be at byte 56.
  Compressed images are not supported. The firmware boots any image with such a header without further validation.

The file is loaded to the base of the first RAM node plus `0x200000`. Both the file, rounded up to whole blocks, and the
header's `image_size` have to fit below the device tree (because Linux still needs to read it).

### Data Packs

Data packs provide flash and disk images under `block_devices/flash`, `block_devices/hdd` and `block_devices/floppy`.
The stock firmware and root image live at `oc2:block_devices/flash/riscv.bin` and `oc2:block_devices/hdd/sedna.bin`,
respectively, and may also be replaced using data packs. Files under `file_systems` are available through the `builtin`
9P device. Also see [datapack.md](datapack.md).
