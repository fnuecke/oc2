# Hard Drive
![Going in circles](item:oc2:hard_drive_large)

Hard drives are the main storage device used for data storage in [computers](../block/computer.md) and [robots](robot.md).

Usually computers use a firmware that requires some hard drive to complete the boot process. See the [flash memory](flash_memory.md) entry for more details. The RISC-V Linux requires a root file system that goes with its firmware. One may be obtained by crafting an 8M hard drive together with a [RISC-V processor](cpu_riscv.md), which writes that root file system to the drive.

Crafting the result with a [wrench](wrench.md) wipes it back to an empty drive.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

Storage media are not perfect, and a drive left unused for a long time may suffer from bit rot and lose its contents. Format corrupted drives using the [wrench](wrench.md). Interestingly, this only seems to happen when there are a lot of storage devices in the world.
