package li.cil.circuity.vm.elf;

public enum ISA {
    UNSPECIFIED(0x00, "No specific instruction set"),
    AT_T_WE_32100(0x01, "AT&T WE 32100"),
    SPARC(0x02, "SPARC"),
    X86(0x03, "x86"),
    MOTOROLA_68000_M68K(0x04, "Motorola 68000 (M68k)"),
    MOTOROLA_88000_M88K(0x05, "Motorola 88000 (M88k)"),
    INTEL_MCU(0x06, "Intel MCU"),
    INTEL_80860(0x07, "Intel 80860"),
    MIPS(0x08, "MIPS"),
    IBM_SYSTEM_370(0x09, "IBM_System/370"),
    MIPS_RS3000_LITTLE_ENDIAN(0x0A, "MIPS RS3000 Little-endian"),
    HEWLETT_PACKARD_PA_RISC(0x0E, "Hewlett-Packard PA-RISC"),
    INTEL_80960(0x13, "Intel 80960"),
    POWERPC(0x14, "PowerPC"),
    POWERPC_64(0x15, "PowerPC (64-bit)"),
    S390_S390X(0x16, "S390, including S390x"),
    ARM(0x28, "ARM (up to ARMv7/Aarch32)"),
    SUPERH(0x2A, "SuperH"),
    IA_64(0x32, "IA-64"),
    AMD64(0x3E, "amd64"),
    TMS320C6000_FAMILY(0x8C, "TMS320C6000 Family"),
    ARM_64(0xB7, "ARM 64-bits (ARMv8/Aarch64)"),
    RISC_V(0xF3, "RISC-V"),

    ;

    public final int value;
    public final String name;

    ISA(final int value, final String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
