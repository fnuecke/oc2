# OpenOS II
OpenOS II runs on the [RISC-V processor](../item/cpu_riscv.md). It is a small operating system with a shell and programs written in Lua. It should feel fairly familiar if you've used OpenOS before. It needs far less memory and storage than [Linux](linux.md), but has no networking.

## Parts
Build the computer as described in [getting started](index.md), using these parts:
- 1x [computer](../block/computer.md)
- 1x [RISC-V processor](../item/cpu_riscv.md)
- 1x **Sedna Firmware** [flash memory](../item/flash_memory.md), crafted from flash memory and a RISC-V processor
- 1x **OpenOS II** [hard drive](../item/hard_drive.md), crafted from a 2M hard drive and a RISC-V processor
- 1x 2M [memory](../item/memory.md)

## Starting Up
Power the computer and turn it on. OpenOS II boots and shows its prompt, `/ # `. There is no login, there are no users. The prompt shows the current directory. Well done, you now have a computer that's ready for use!

## The System
The shell works much like one would on Linux:
- `ls /bin /mnt/builtin/bin` lists the installed programs. Most programs print their usage with `--help`.
- Up and Down go through the history, Tab completes commands and paths.
- `|` passes the output of one program to the next, `>` and `>>` write it to a file, `<` reads from one.
- `;`, `&&` and `||` chain commands. `*` and `?` match file names.
- `cmd &` runs a command in the background. `jobs` lists these, `kill` ends one.
- Ctrl+C stops the running program.
- To turn the computer off, run `shutdown`. `reboot` restarts it.

Commands placed in `/.shrc` run whenever the shell starts, e.g. aliases (`alias ll='ls -l'`) or a keyboard layout.

## Writing Programs
To edit a file, run `edit <file>`. Ctrl+S saves, Ctrl+W closes.

A program is a Lua file. Use `lua <file>` to run them, `lua` alone starts the interactive prompt. Programs in `/bin`, `/mnt/builtin/bin` and the current directory run by name, without the `.lua` extension. The program gets its arguments as `...`:

`for i, name in ipairs({...}) do`  
`  print(i, name)`  
`end`

`require` loads libraries from `/lib`, from the built-in file system at `/mnt/builtin/lib/lua`, and from the current directory.

## Drives
The boot drive is `/dev/vda`. Additional [hard drives](../item/hard_drive.md), [disk drives](../block/disk_drive.md) and [flash drives](../block/flash_drive.md) follow as `/dev/vdb`, `/dev/vdc` and so on.

Drives are mounted automatically, at `/mnt/<label>` if the medium has a label, else at `/mnt/1`, `/mnt/2` and so on.

A new drive needs a file system first. Run `format /dev/vdb` to create one, optionally passing a label as a second argument. It's also possible to use `label` to change the label later. Formatting a drive *erases all data on it!*

Floppies can be swapped at any time. Close open files on a floppy before removing it, to avoid data loss.

## Devices
To control devices such as the [redstone interface](../block/redstone_interface.md), use the `devices` library, see the [high-level API](../hlapi.md). `lsdev` lists the connected devices.

The built-in file system at `/mnt/builtin` also provides `import` and `export` for the [file import/export card](../item/file_import_export_card.md), and the `robot` library for [robots](../robotics.md).

The [sound card](../item/sound_card.md), [projector](../block/projector.md) and [keyboard](../block/keyboard.md) are available to programs. See their respective entries on how to use them.

## Clocks
Computers provide two hardware clock (RTC) devices. The first one counts time in a scale most users will think in. It is used by default, for example by `date` and `time`. The second one measures time as it works in the world the computer runs in. To obtain the current world time, use `date -w`.
