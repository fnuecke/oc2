package li.cil.circuity.vm.riscv.device;

import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;

public final class R5HostTargetInterface implements MemoryMappedDevice {
    public static final int COMMAND_POWER_OFF = 1;

    private long toHost, fromHost;

    @Override
    public int getLength() {
        return 16;
    }

    @Override
    public int load(final int offset, final int sizeLog2) {
        assert sizeLog2 == 2;
        switch (offset) {
            case 0: {
                return (int) toHost;
            }
            case 4: {
                return (int) (toHost >> 32);
            }

            case 8: {
                return (int) fromHost;
            }
            case 12: {
                return (int) (fromHost >> 32);
            }
            default: {
                return 0;
            }
        }
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) {
        assert sizeLog2 == 2;
        switch (offset) {
            case 0: {
                toHost = (toHost & ~0xFFFFFFFFL) | value;
                break;
            }
            case 4: {
                toHost = (toHost & 0xFFFFFFFFL) | ((long) value << 32);
                handleCommand();
                break;
            }

            case 8: {
                fromHost = (fromHost & ~0xFFFFFFFFL) | value;
                break;
            }
            case 12: {
                fromHost = (fromHost & 0xFFFFFFFFL) | ((long) value << 32);
                break;
            }
        }
    }

    private void handleCommand() {
        final int device = (int) (toHost >>> 56);
        final int cmd = (int) (toHost >>> 48) & 0xff;
        if (toHost == COMMAND_POWER_OFF) { // request power off
            System.out.println("power off");
            // todo stop vm
        } else if (device == 1 && cmd == 1) { // console output
//            console.write_data(toHost & 0xff);
            System.out.print((char) (toHost & 0xFF));
            toHost = 0;
            fromHost = ((long) device << 56) | ((long) cmd << 48);
        } else if (device == 1 && cmd == 0) { // request keyboard interrupt
            toHost = 0;
        } else {
            System.out.printf("HTIF: unsupported tohost=0x%016x\n", toHost);
        }
    }
}
