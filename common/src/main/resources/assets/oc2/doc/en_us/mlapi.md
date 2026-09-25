# Mid-level API
Programs running on a [Z80 processor](item/cpu_z80.md) access devices through the mid-level API (MLAPI). It usually makes available a subset of the devices accessible via the [high-level API](hlapi.md) (HLAPI), in a form an 8-bit machine can drive: a function is picked by number, and its arguments and results are plain bytes.

For the function codes each device offers, see the [list of devices](device/index.md).

Everything here is done through ports, so it works from assembly. Two libraries on the boot disk save you writing the tedious parts, `DEVLIB.INC` for finding general devices and `OCAPI.INC` for finding and calling mid-level API devices. Include them at the end of your sources:  
`INCLUDE DEVLIB.INC`  
`INCLUDE OCAPI.INC`

## Finding a Device
Devices announce themselves through an enumeration window at a fixed port. `DEVS.COM` prints what's on there, which is a good first check that a device is connected at all.

Every MLAPI device answers on the same port and is told apart by a MLAPI-device index. `OCFIND` in `OCAPI.INC` does the lookup by name:
- `HL` points at the name, up to six characters, terminated by a zero
- `B` is which one to find, counting from zero, for when several share the same name
- On success the carry flag is clear, `A` holds the port to talk to and `E` holds the MLAPI-device index
- On failure the carry flag is set

Write `E` to the `OCSEL` register to pick the device, then work from the port in `A`. Selecting stays valid until you select something else. Read the index again after devices change on the bus, since it can shift.

## Making a Call
The port `OCFIND` returned has four registers, named in `OCAPI.INC`:
- `OCSEL` selects the MLAPI-device
- `OCFUN` takes a function code and starts a call
- `OCDAT` takes argument bytes, and gives back result bytes
- `OCSTA` reports status, and takes `OCEXEC` to run the call or `OCABRT` to abandon it

A call goes: write the function code to `OCFUN`, write each argument byte to `OCDAT`, write `OCEXEC` to `OCSTA`. Then poll `OCSTA` until `OCBUSY` clears, and read result bytes from `OCDAT` while `OCDAV` is set.

Many calls may take some time to execute, so make sure to poll and don't just continue. Only one call runs at a time, and while `OCBUSY` is set only writes to `OCSTA` are accepted.

When `OCERR` is set in the status, the call failed and `OCDAT` gives the reason:
- `OCENOD`, no such MLAPI-device
- `OCENOF`, no such function
- `OCEARG`, the arguments were not what the function wanted
- `OCEINT`, the device failed internally

The registers hold a single selection and a single call, so keep to one at a time and leave them out of interrupt handlers.

## Example
`REDSTN.Z80` on the boot disk drives a [redstone interface](block/redstone_interface.md), and its comments include the equivalent C, if that reads more easily. Writing to a floppy needs a [disk drive](block/disk_drive.md). Build it while staying on `A:`:  
`ZMAC REDSTN /OB:REDSTN /E`  
`ZML B:REDSTN`

The first command assembles the source. `/O` puts the result in `B:REDSTN.REL` and `/E` drops the error log. The second links that into `B:REDSTN.COM`, which CP/M can run:  
`B:REDSTN 1 15`

This sets the output on side 1, upwards, to 15. Put a redstone lamp above the computer and it lights up. Leave off the level to read the levels back instead:  
`B:REDSTN 1`

To write your own, `ED` on the boot disk creates and edits source files, as in `ED B:PROG.Z80`.
