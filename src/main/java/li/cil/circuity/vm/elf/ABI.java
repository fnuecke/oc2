package li.cil.circuity.vm.elf;

public enum ABI {
    SYSTEM_V(0x00, "System V"),
    HP_UX(0x01, "HP-UX"),
    NETBSD(0x02, "NetBSD"),
    LINUX(0x03, "Linux"),
    GNU_HURD(0x04, "GNU Hurd"),
    SOLARIS(0x06, "Solaris"),
    AIX(0x07, "AIX"),
    IRIX(0x08, "IRIX"),
    FREEBSD(0x09, "FreeBSD"),
    TRU64(0x0A, "Tru64"),
    NOVELL_MODESTO(0x0B, "Novell Modesto"),
    OPENBSD(0x0C, "OpenBSD"),
    OPENVMS(0x0D, "OpenVMS"),
    NONSTOP_KERNEL(0x0E, "NonStop Kernel"),
    AROS(0x0F, "AROS"),
    FENIX_OS(0x10, "Fenix OS"),
    CLOUDABI(0x11, "CloudABI"),
    STRATUS_TECHNOLOGIES_OPENVOS(0x12, "Stratus Technologies OpenVOS"),

    ;

    public final int value;
    public final String name;

    ABI(final int value, final String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
