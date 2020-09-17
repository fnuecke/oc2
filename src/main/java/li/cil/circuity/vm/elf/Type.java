package li.cil.circuity.vm.elf;

public enum Type {
    ET_NONE(0x00),
    ET_REL(0x01),
    ET_EXEC(0x02),
    ET_DYN(0x03),
    ET_CORE(0x04),

    ;

    public final int value;

    Type(final int value) {
        this.value = value;
    }
}
