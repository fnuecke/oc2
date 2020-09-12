package li.cil.circuity.vm.riscv;

@SuppressWarnings("SpellCheckingInspection")
public final class R5CPUStateSnapshot {
    public int pc;
    public final int[] x = new int[32];

//    public final float[] f = new float[32];
//    public byte fflags;
//    public byte frm;

    public int reservation_set = -1;

    public long mcycle;

    public int mstatus;
    public int mtvec;
    public int medeleg, mideleg;
    public int mip, mie;
    public int mcounteren;
    public int mscratch;
    public int mepc;
    public int mcause;
    public int mtval;
    public byte fs;

    public int stvec;
    public int scounteren;
    public int sscratch;
    public int sepc;
    public int scause;
    public int stval;
    public int satp;

    public int priv;
    public boolean waitingForInterrupt;
}
