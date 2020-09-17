package li.cil.circuity.vm.elf;

public enum ProgramHeaderType {
    PT_NULL(0x00000000),
    PT_LOAD(0x00000001),
    PT_DYNAMIC(0x00000002),
    PT_INTERP(0x00000003),
    PT_NOTE(0x00000004),
    PT_SHLIB(0x00000005),
    PT_PHDR(0x00000006),
    PT_TLS(0x00000007),

    ;

    public final int value;

    ProgramHeaderType(final int value) {
        this.value = value;
    }
}
