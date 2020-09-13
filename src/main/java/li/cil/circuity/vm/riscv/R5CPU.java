package li.cil.circuity.vm.riscv;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.MemoryRange;
import li.cil.circuity.api.vm.device.InterruptController;
import li.cil.circuity.api.vm.device.Steppable;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.rtc.RealTimeCounter;
import li.cil.circuity.vm.device.memory.exception.*;
import li.cil.circuity.vm.riscv.exception.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * RISC-V
 * <p>
 * Based on ISA specifications found at https://riscv.org/technical/specifications/
 * <ul>
 * <li>Volume 1, Unprivileged Spec v.20191213</li>
 * <li>Volume 2, Privileged Spec v.20190608</li>
 * </ul>
 * <p>
 * Implemented extensions:
 * <ul>
 * <li>RV32I Base Integer Instruction Set, Version 2.1</li>
 * <li>"Zifencei" Instruction-Fetch Fence, Version 2.0</li>
 * <li>"M" Standard Extension for Integer Multiplication and Division, Version 2.0</li>
 * <li>"A" Standard Extension for Atomic Instructions, Version 2.1</li>
 * <li>"Zicsr", Control and Status Register (CSR) Instructions, Version 2.0</li>
 * <li>TODO "F" Standard Extension for Single-Precision Floating-Point, Version 2.2</li>
 * <li>TODO "D"</li>
 * <li>"C" Standard Extension for Compressed Instructions, Version 2.0</li>
 * </ul>
 */
public class R5CPU implements Steppable, InterruptController {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int PC_INIT = 0x1000; // Initial position of program counter.

    private static final int XLEN = 32; // Integer register width.

    // Base ISA descriptor CSR (misa) (V2p16).
    private static final int MISA = (R5.mxl(XLEN) << (XLEN - 2)) | R5.isa('I', 'M', 'A', 'S', 'U', 'C'); // 'F', 'D'

    // UBE, SBE, MBE hardcoded to zero for little endianness.
    // SD is computed.
    // TVM, TW, TSR are unsupported and hardwired to zero.
    private static final int MSTATUS_MASK = (R5.STATUS_UIE_MASK | R5.STATUS_SIE_MASK | R5.STATUS_MIE_MASK |
                                             R5.STATUS_UPIE_MASK | R5.STATUS_SPIE_MASK | R5.STATUS_MPIE_MASK |
                                             R5.STATUS_SPP_MASK | R5.STATUS_MPP_MASK |
                                             R5.STATUS_FS_MASK |
                                             R5.STATUS_MPRV_MASK | R5.STATUS_SUM_MASK | R5.STATUS_MXR_MASK);

    // No time and no high perf counters.
    private static final int COUNTEREN_MASK = R5.MCOUNTERN_CY | R5.MCOUNTERN_IR;

    // Supervisor status (sstatus) CSR mask over mstatus.
    private static final int SSTATUS_MASK = (R5.STATUS_UIE_MASK | R5.STATUS_SIE_MASK |
                                             R5.STATUS_UPIE_MASK | R5.STATUS_SPIE_MASK |
                                             R5.STATUS_SPP_MASK |
                                             R5.STATUS_FS_MASK | R5.STATUS_XS_MASK |
                                             R5.STATUS_SUM_MASK | R5.STATUS_MXR_MASK);

    // Translation look-aside buffer config.
    private static final int TLB_SIZE = 256; // Must be a power of two for fast modulo via `& (TLB_SIZE - 1)`.

    ///////////////////////////////////////////////////////////////////
    // RV32I
    private int pc; // Program counter.
    private final int[] x = new int[32]; // Integer registers.

    ///////////////////////////////////////////////////////////////////
    // RV32F
//    private final float[] f = new float[32]; // Float registers.
//    private byte fflags; // fcsr[4:0] := NV . DZ . OF . UF . NX
//    private byte frm; // fcsr[7:5]

    ///////////////////////////////////////////////////////////////////
    // RV32A
    private int reservation_set = -1; // Reservation set for RV32A's LR/SC.

    ///////////////////////////////////////////////////////////////////
    // User-level CSRs
    private long mcycle;

    // Machine-level CSRs
    private int mstatus; // Machine Status Register; mstatush is always zero for us, SD is computed
    private int mtvec; // Machine Trap-Vector Base-Address Register; 0b11=Mode: 0=direct, 1=vectored
    private int medeleg, mideleg; // Machine Trap Delegation Registers
    private int mip, mie; // Machine Interrupt Registers
    private int mcounteren; // Machine Counter-Enable Register
    private int mscratch; // Machine Scratch Register
    private int mepc; // Machine Exception Program Counter
    private int mcause; // Machine Cause Register
    private int mtval; //  Machine Trap Value Register
    private byte fs; // part of mstatus, store separate for convenience

    // Supervisor-level CSRs
    private int stvec; // Supervisor Trap Vector Base Address Register; 0b11=Mode: 0=direct, 1=vectored
    private int scounteren; // Supervisor Counter-Enable Register
    private int sscratch; // Supervisor Scratch Register
    private int sepc; // Supervisor Exception Program Counter
    private int scause; // Supervisor Cause Register
    private int stval; // Supervisor Trap Value Register
    private int satp; // Supervisor Address Translation and Protection Register

    ///////////////////////////////////////////////////////////////////
    // Misc. state
    private int priv; // Current privilege level.
    private boolean waitingForInterrupt;

    ///////////////////////////////////////////////////////////////////
    // Memory access

    // Translation look-aside buffers.
    private final TLBEntry[] tlb_fetch = new TLBEntry[TLB_SIZE];
    private final TLBEntry[] tlb_read = new TLBEntry[TLB_SIZE];
    private final TLBEntry[] tlb_write = new TLBEntry[TLB_SIZE];

    // Access to physical memory for load/store operations.
    private final MemoryMap physicalMemory;

    // Real time counter -- at least in RISC-V Linux 5.1 the mtime CSR is needed in add_device_randomness
    // where it doesn't use the SBI. Not implementing it would cause an illegal instruction exception
    // halting the system.
    private final RealTimeCounter rtc;

    public R5CPU(final RealTimeCounter rtc, final MemoryMap physicalMemory) {
        this.rtc = rtc;
        this.physicalMemory = physicalMemory;

        for (int i = 0; i < TLB_SIZE; i++) {
            tlb_fetch[i] = new TLBEntry();
        }
        for (int i = 0; i < TLB_SIZE; i++) {
            tlb_read[i] = new TLBEntry();
        }
        for (int i = 0; i < TLB_SIZE; i++) {
            tlb_write[i] = new TLBEntry();
        }

        reset();
    }

    public void reset() {
        pc = PC_INIT;

        // Volume 2, 3.3 Reset
        priv = R5.PRIVILEGE_M;
        mstatus = mstatus & ~R5.STATUS_MIE_MASK;
        mstatus = mstatus & ~R5.STATUS_MPRV_MASK;
        mcause = 0;

        flushTLB();
    }

    public void raiseInterrupts(final int mask) {
        mip |= mask;
        if (waitingForInterrupt && (mip & mie) != 0) {
            waitingForInterrupt = false;
        }
    }

    public void lowerInterrupts(final int mask) {
        mip &= ~mask;
    }

    public void step(final int cycles) {
        final long cycleLimit = mcycle + cycles;
        // Note: practically the same as as cycleLimit > cycles, but we make sure the delta is
        // sufficiently small to be a positive integer.
        while (!waitingForInterrupt && cycleLimit > mcycle) {
            if ((mip & mie) != 0 && raiseInterrupt()) {
                continue;
            }

            try {
                step();
            } catch (final LoadPageFaultException e) {
                raiseException(R5.EXCEPTION_LOAD_PAGE_FAULT, e.getAddress());
            } catch (final StorePageFaultException e) {
                raiseException(R5.EXCEPTION_STORE_PAGE_FAULT, e.getAddress());
            } catch (final FetchPageFaultException e) {
                raiseException(R5.EXCEPTION_FETCH_PAGE_FAULT, e.getAddress());
            } catch (final LoadFaultException e) {
                raiseException(R5.EXCEPTION_FAULT_LOAD, e.getAddress());
            } catch (final StoreFaultException e) {
                raiseException(R5.EXCEPTION_FAULT_STORE, e.getAddress());
            } catch (final FetchFaultException e) {
                raiseException(R5.EXCEPTION_FAULT_FETCH, e.getAddress());
            } catch (final MisalignedLoadException e) {
                raiseException(R5.EXCEPTION_MISALIGNED_LOAD, e.getAddress());
            } catch (final MisalignedStoreException e) {
                raiseException(R5.EXCEPTION_MISALIGNED_STORE, e.getAddress());
            } catch (final MisalignedFetchException e) {
                raiseException(R5.EXCEPTION_MISALIGNED_FETCH, e.getAddress());
            } catch (final MemoryAccessException e) {
                throw new AssertionError();
            } catch (final R5Exception e) {
                raiseException(e.getExceptionCause(), e.getExceptionValue());
            }

            assert x[0] == 0;
        }
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void step() throws R5Exception, MemoryAccessException {
        final int inst = fetch(pc);

        mcycle++;

        if ((inst & 0b11) == 0b11) {
            // Instruction decoding, see Volume I: RISC-V Unprivileged ISA V20191214-draft page 16ff.

            final int opcode = getField(inst, 0, 6, 0);
            final int rd = getField(inst, 7, 11, 0);
            final int rs1 = getField(inst, 15, 19, 0);
            final int rs2 = getField(inst, 20, 24, 0);
            final int funct3 = getField(inst, 12, 14, 0);

            // Opcode values, see Volume I: RISC-V Unprivileged ISA V20191214-draft page 130ff. They appear in the order
            // they are introduced in the spec. Immediate value decoding follows the layouts described in 2.3.

            switch (opcode) {
                ///////////////////////////////////////////////////////////////////
                // 2.4 Integer Computational Instructions
                ///////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////
                // Integer Register-Immediate Instructions
                case 0b0010011: { // OP-IMM (register-immediate operation)
                    final int imm = inst >> 20; // inst[31:20], sign extended
                    final int result;
                    switch (funct3) {
                        case 0b000: { // ADDI
                            result = x[rs1] + imm;
                            break;
                        }
                        case 0b010: { // SLTI
                            result = x[rs1] < imm ? 1 : 0;
                            break;
                        }
                        case 0b011: { // SLTIU
                            result = Integer.compareUnsigned(x[rs1], imm) < 0 ? 1 : 0;
                            break;
                        }
                        case 0b100: { // XORI
                            result = x[rs1] ^ imm;
                            break;
                        }
                        case 0b110: { // ORI
                            result = x[rs1] | imm;
                            break;
                        }
                        case 0b111: { // ANDI
                            result = x[rs1] & imm;
                            break;
                        }
                        case 0b001: { // SLLI
                            if ((inst & 0b1111111_00000_00000_000_00000_0000000) != 0)
                                throw new R5IllegalInstructionException(inst);

                            result = x[rs1] << (imm & 0b11111);
                            break;
                        }
                        case 0b101: { // SRLI/SRAI
                            final int funct7 = getField(imm, 5, 11, 0); // imm[11:5]
                            switch (funct7) {
                                case 0b0000000: { // SRLI
                                    result = x[rs1] >>> (imm & 0b11111);
                                    break;
                                }
                                case 0b0100000: { // SRAI
                                    result = x[rs1] >> (imm & 0b11111);
                                    break;
                                }

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }
                            break;
                        }

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    if (rd != 0) {
                        x[rd] = result;
                    }

                    pc += 4;
                    break;
                }

                case 0b0110111: { // LUI
                    final int imm = inst & 0b11111111111111111111_00000_0000000; // inst[31:12]
                    if (rd != 0) {
                        x[rd] = imm;
                    }

                    pc += 4;
                    break;
                }

                case 0b0010111: { // AUIPC
                    final int imm = inst & 0b11111111111111111111_00000_0000000; // inst[31:12]
                    if (rd != 0) {
                        x[rd] = pc + imm;
                    }

                    pc += 4;
                    break;
                }

                case 0b0110011: { // OP (register-register operation aka R-type)
                    final int a = x[rs1];
                    final int b = x[rs2];
                    final int funct7 = getField(inst, 25, 31, 0);
                    switch (funct7) {
                        case 0b000001: {
                            ///////////////////////////////////////////////////////////////////
                            // Chapter 7 "M" Standard Extension for Integer Multiplication and Division, Version 2.0
                            ///////////////////////////////////////////////////////////////////

                            final int result;
                            switch (funct3) {
                                ///////////////////////////////////////////////////////////////////
                                // 7.1 Multiplication Operations

                                case 0b000: { // MUL
                                    result = a * b;
                                    break;
                                }
                                case 0b001: { // MULH
                                    result = (int) (((long) a * (long) b) >> 32);
                                    break;
                                }
                                case 0b010: { // MULHSU
                                    result = (int) (((long) a * Integer.toUnsignedLong(b)) >> 32);
                                    break;
                                }
                                case 0b011: { // MULHU
                                    result = (int) ((Integer.toUnsignedLong(a) * Integer.toUnsignedLong(b)) >>> 32);
                                    break;
                                }

                                ///////////////////////////////////////////////////////////////////
                                // 7.2 Division Operations, special cases from table 7.1 on p45.

                                case 0b100: { // DIV
                                    if (b == 0) {
                                        result = -1;
                                    } else if (a == Integer.MIN_VALUE && b == -1) {
                                        result = a;
                                    } else {
                                        result = a / b;
                                    }
                                    break;
                                }
                                case 0b101: { // DIVU
                                    if (b == 0) {
                                        result = Integer.MAX_VALUE;
                                    } else {
                                        result = Integer.divideUnsigned(a, b);
                                    }
                                    break;
                                }
                                case 0b110: { // REM
                                    if (b == 0) {
                                        result = a;
                                    } else if (a == Integer.MIN_VALUE && b == -1) {
                                        result = 0;
                                    } else {
                                        result = a % b;
                                    }
                                    break;
                                }
                                case 0b111: { // REMU
                                    if (b == 0) {
                                        result = a;
                                    } else {
                                        result = Integer.remainderUnsigned(a, b);
                                    }
                                    break;
                                }

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }

                            if (rd != 0) {
                                x[rd] = result;
                            }

                            break;
                        }
                        case 0b0000000:
                        case 0b0100000: { // Integer Register-Register Operations
                            final int result;
                            switch (funct3 | funct7) {
                                case 0b000: { // ADD
                                    result = a + b;
                                    break;
                                }
                                //noinspection PointlessBitwiseExpression
                                case 0b000 | 0b0100000: { // SUB
                                    result = a - b;
                                    break;
                                }
                                case 0b001: { // SLL
                                    result = a << b;
                                    break;
                                }
                                case 0b010: { // SLT
                                    result = a < b ? 1 : 0;
                                    break;
                                }
                                case 0b011: { // SLTU
                                    result = Integer.compareUnsigned(a, b) < 0 ? 1 : 0;
                                    break;
                                }
                                case 0b100: { // XOR
                                    result = a ^ b;
                                    break;
                                }
                                case 0b101: { // SRL
                                    result = a >>> b;
                                    break;
                                }
                                case 0b101 | 0b0100000: { // SRA
                                    result = a >> b;
                                    break;
                                }
                                case 0b110: { // OR
                                    result = a | b;
                                    break;
                                }
                                case 0b111: { // AND
                                    result = a & b;
                                    break;
                                }

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }

                            if (rd != 0) {
                                x[rd] = result;
                            }

                            break;
                        }

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    pc += 4;
                    break;
                }

                ///////////////////////////////////////////////////////////////////
                // 2.5 Control Transfer Instructions
                ///////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////
                // Unconditional Jumps
                case 0b1101111: { // JAL
                    final int imm = extendSign(getField(inst, 31, 31, 20) |
                                               getField(inst, 21, 30, 1) |
                                               getField(inst, 20, 20, 11) |
                                               getField(inst, 12, 19, 12), 21);
                    if (rd != 0) {
                        x[rd] = pc + 4;
                    }

                    pc += imm;
                    break;
                }

                case 0b110_0111: { // JALR
                    final int imm = inst >> 20; // inst[31:20], sign extended
                    final int address = (x[rs1] + imm) & ~0b1; // Compute first in case rs1 == rd.
                    // Note: we just mask here, but technically we should raise an exception for misaligned jumps.
                    if (rd != 0) {
                        x[rd] = pc + 4;
                    }

                    pc = address;
                    break;
                }

                ///////////////////////////////////////////////////////////////////
                // Conditional Branches
                case 0b1100011: { // BRANCH
                    final boolean invert = (funct3 & 0b1) != 0;

                    final boolean condition;
                    switch (funct3 >>> 1) {
                        case 0b00: { // BEQ / BNE
                            condition = x[rs1] == x[rs2];
                            break;
                        }
                        case 0b10: { // BLT / BGE
                            condition = x[rs1] < x[rs2];
                            break;
                        }
                        case 0b11: { // BLTU / BGEU
                            condition = Integer.compareUnsigned(x[rs1], x[rs2]) < 0;
                            break;
                        }
                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    if (condition ^ invert) {
                        final int imm = extendSign(getField(inst, 31, 31, 12) |
                                                   getField(inst, 25, 30, 5) |
                                                   getField(inst, 8, 11, 1) |
                                                   getField(inst, 7, 7, 11), 13);

                        pc += imm;
                    } else {
                        pc += 4;
                    }

                    break;
                }

                ///////////////////////////////////////////////////////////////////
                // 2.6 Load and Store Instructions
                ///////////////////////////////////////////////////////////////////

                case 0b0000011: { // LOAD
                    final int imm = inst >> 20; // inst[31:20], sign extended
                    final int address = x[rs1] + imm;

                    final int result;
                    switch (funct3) {
                        case 0b000: { // LB
                            result = load8(address);
                            break;
                        }
                        case 0b001: { // LH
                            result = load16(address);
                            break;
                        }
                        case 0b010: { // LW
                            result = load32(address);
                            break;
                        }
                        case 0b100: { // LBU
                            result = load8(address) & 0xFF;
                            break;
                        }
                        case 0b101: { // LHU
                            result = load16(address) & 0xFFFF;
                            break;
                        }

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    if (rd != 0) {
                        x[rd] = result;
                    }

                    pc += 4;
                    break;
                }

                case 0b0100011: { // STORE
                    final int imm = extendSign(getField(inst, 25, 31, 5) |
                                               getField(inst, 7, 11, 0), 12);

                    final int address = x[rs1] + imm;
                    final int value = x[rs2];

                    switch (funct3) {
                        case 0b000: { // SB
                            store8(address, (byte) value);
                            break;
                        }
                        case 0b001: { // SH
                            store16(address, (short) value);
                            break;
                        }
                        case 0b010: { // SW
                            store32(address, value);
                            break;
                        }
                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    pc += 4;
                    break;
                }

                ///////////////////////////////////////////////////////////////////
                // 2.7 Memory Ordering Instructions
                ///////////////////////////////////////////////////////////////////

                case 0b0001111: { // MISC-MEM
                    switch (funct3) {
                        case 0b000: { // FENCE
//                        if ((inst & 0b1111_00000000_11111_111_11111_0000000) != 0) // Not supporting any flags.
//                            throw new IllegalInstructionException(inst);
                            break;
                        }

                        ///////////////////////////////////////////////////////////////////
                        // Chapter 3: "Zifencei" Instruction-Fetch Fence, Version 2.0
                        ///////////////////////////////////////////////////////////////////

                        case 0b001: { // FENCE.I
                            if (inst != 0b000000000000_00000_001_00000_0001111)
                                throw new R5IllegalInstructionException(inst);
                            break;
                        }
                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    pc += 4;
                    break;
                }

                ///////////////////////////////////////////////////////////////////
                // 2.8 Environment Call and Breakpoints
                ///////////////////////////////////////////////////////////////////

                case 0b1110011: { // SYSTEM
                    if (funct3 == 0b100) {
                        throw new R5IllegalInstructionException(inst);
                    }

                    switch (funct3 & 0b11) {
                        case 0b00: { // PRIV
                            final int funct12 = inst >>> 20; // inst[31:20], not sign-extended
                            switch (funct12) {
                                case 0b0000000_00000: { // ECALL
                                    if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    throw new R5ECallException(priv);
                                }
                                case 0b0000000_00001: { // EBREAK
                                    if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    throw new R5BreakpointException();
                                }
                                // 0b0000000_00010: URET
                                case 0b0001000_00010: { // SRET
                                    if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }
                                    if (priv < R5.PRIVILEGE_S) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    sret();
                                    return;
                                }
                                case 0b0011000_00010: { // MRET
                                    if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }
                                    if (priv < R5.PRIVILEGE_M) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    mret();
                                    return;
                                }

                                case 0b0001000_00101: { // WFI
                                    if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }
                                    if (priv == R5.PRIVILEGE_U) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    if ((mip & mie) == 0) {
                                        waitingForInterrupt = true;
                                        pc += 4;
                                        return;
                                    }
                                    break;
                                }

                                default: {
                                    final int funct7 = funct12 >>> 5;
                                    if (funct7 == 0b0001001) { // SFENCE.VMA
                                        if ((inst & 0b0000000_00000_00000_111_11111_0000000) != 0) {
                                            throw new R5IllegalInstructionException(inst);
                                        }
                                        if (priv == R5.PRIVILEGE_U) {
                                            throw new R5IllegalInstructionException(inst);
                                        }

                                        if (rs1 == 0) {
                                            flushTLB();
                                        } else {
                                            flushTLB(x[rs1]);
                                        }
                                    } else {
                                        throw new R5IllegalInstructionException(inst);
                                    }
                                    break;
                                }
                            }
                        }

                        ///////////////////////////////////////////////////////////////////
                        // Chapter 9 "Zicsr", Control and Status Register (CSR) Instructions, Version 2.0
                        ///////////////////////////////////////////////////////////////////

                        case 0b01: // CSRRW[I]
                        case 0b10: // CSRRS[I]
                        case 0b11: { // CSRRC[I]
                            final int csr = inst >>> 20; // inst[31:20], not sign-extended
                            final int a = (funct3 & 0b100) == 0 ? x[rs1] : rs1; // 0b1XX are immediate versions.
                            final int funct3lb = funct3 & 0b11;
                            switch (funct3lb) {
                                case 0b01: { // CSRRW[I]
                                    checkCSR(inst, csr, true);
                                    if (rd != 0) { // Explicit check, spec says no read side-effects when rd = 0.
                                        final int b = readCSR(inst, csr);
                                        writeCSR(inst, csr, a);
                                        x[rd] = b; // Write to register last, avoid lingering side-effect when write errors.
                                    } else {
                                        writeCSR(inst, csr, a);
                                    }
                                    break;
                                }
                                case 0b10:  // CSRRS[I]
                                case 0b11: { // CSRRC[I]
                                    final int b;
                                    if (rs1 != 0) {
                                        checkCSR(inst, csr, true);
                                        b = readCSR(inst, csr);
                                        final int masked = funct3lb == 0b10 ? (a | b) : (~a & b);
                                        writeCSR(inst, csr, masked);
                                    } else {
                                        checkCSR(inst, csr, false);
                                        b = readCSR(inst, csr);
                                    }

                                    if (rd != 0) {
                                        x[rd] = b;
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    pc += 4;
                    break;
                }

                ///////////////////////////////////////////////////////////////////
                // Chapter 8 "A" Standard Extension for Atomic Instructions, Version 2.1
                ///////////////////////////////////////////////////////////////////

                case 0b0101111: { // AMO
                    switch (funct3) { // width
                        case 0b010: { // 32
                            final int funct5 = inst >>> 27; // inst[31:27], not sign-extended
                            final int address = x[rs1];
                            final int result;
                            switch (funct5) {
                                ///////////////////////////////////////////////////////////////////
                                // 8.2 Load-Reserved/Store-Conditional Instructions
                                case 0b00010: { // LR.W
                                    if (rs2 != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }
                                    result = load32(address);
                                    reservation_set = address;
                                    break;
                                }
                                case 0b00011: { // SC.W
                                    if (address == reservation_set) {
                                        store32(address, x[rs2]);
                                        result = 0;
                                    } else {
                                        result = 1;
                                    }
                                    reservation_set = -1; // Always invalidate as per spec.
                                    break;
                                }

                                ///////////////////////////////////////////////////////////////////
                                // 8.4 Atomic Memory Operations
                                case 0b00001: // AMOSWAP.W
                                case 0b00000: // AMOADD.W
                                case 0b00100: // AMOXOR.W
                                case 0b01100: // AMOAND.W
                                case 0b01000: // AMOOR.W
                                case 0b10000: // AMOMIN.W
                                case 0b10100: // AMOMAX.W
                                case 0b11000: // AMOMINU.W
                                case 0b11100: { // AMOMAXU.W
                                    // Grab operands, load left-hand from memory, right-hand from register.
                                    final int a = load32(address);
                                    final int b = x[rs2];

                                    // Perform atomic operation.
                                    final int c;
                                    switch (funct5) {
                                        case 0b00001: { // AMOSWAP.W
                                            c = b;
                                            break;
                                        }
                                        case 0b00000: { // AMOADD.W
                                            c = a + b;
                                            break;
                                        }
                                        case 0b00100: { // AMOXOR.W
                                            c = a ^ b;
                                            break;
                                        }
                                        case 0b01100: { // AMOAND.W
                                            c = a & b;
                                            break;
                                        }
                                        case 0b01000: { // AMOOR.W
                                            c = a | b;
                                            break;
                                        }
                                        case 0b10000: { // AMOMIN.W
                                            c = Math.min(a, b);
                                            break;
                                        }
                                        case 0b10100: { // AMOMAX.W
                                            c = Math.max(a, b);
                                            break;
                                        }
                                        case 0b11000: { // AMOMINU.W
                                            c = (int) Math.min(Integer.toUnsignedLong(a), Integer.toUnsignedLong(b));
                                            break;
                                        }
                                        case 0b11100: { // AMOMAXU.W
                                            c = (int) Math.max(Integer.toUnsignedLong(a), Integer.toUnsignedLong(b));
                                            break;
                                        }

                                        default: {
                                            throw new R5IllegalInstructionException(inst);
                                        }
                                    }

                                    // Store value read from memory in register, write result back to memory.
                                    result = a;
                                    store32(address, c);
                                    break;
                                }

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }

                            if (rd != 0) {
                                x[rd] = result;
                            }

                            break;
                        }
                        case 0b011: { // 64
                            throw new R5IllegalInstructionException(inst);
                        }

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    pc += 4;
                    break;
                }

                default: {
                    throw new R5IllegalInstructionException(inst);
                }
            }
        } else {
            // Compressed instruction decoding, V1p97ff, p112f.

            if (inst == 0) { // Defined illegal instruction.
                throw new R5IllegalInstructionException(inst);
            }

            final int op = inst & 0b11;
            switch (op) {
                case 0b00: { // Quadrant 0
                    final int funct3 = getField(inst, 13, 15, 0);
                    final int rd = getField(inst, 2, 4, 0) + 8; // V1p100
                    switch (funct3) {
                        case 0b000: { // C.ADDI4SPN
                            final int imm = getField(inst, 11, 12, 4) |
                                            getField(inst, 7, 10, 6) |
                                            getField(inst, 6, 6, 2) |
                                            getField(inst, 5, 5, 3);
                            if (imm == 0) {
                                throw new R5IllegalInstructionException(inst);
                            }

                            x[rd] = x[2] + imm;
                            break;
                        }
                        // 0b001: C.FLD
                        case 0b010: { // C.LW
                            final int offset = getField(inst, 10, 12, 3) |
                                               getField(inst, 6, 6, 2) |
                                               getField(inst, 5, 5, 6);
                            final int rs1 = getField(inst, 7, 9, 0) + 8; // V1p100
                            x[rd] = load32(x[rs1] + offset);
                            break;
                        }
                        // 0b011: C.FLW
                        // 0b101: C.FSD
                        case 0b110: { // C.SW
                            final int offset = getField(inst, 10, 12, 3) |
                                               getField(inst, 6, 6, 2) |
                                               getField(inst, 5, 5, 6);
                            final int rs1 = getField(inst, 7, 9, 0) + 8; // V1p100
                            store32(x[rs1] + offset, x[rd /* = rs2 */]);
                            break;
                        }
                        // 0b111: C.FSW

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    pc += 2;
                    break;
                }

                case 0b01: { // Quadrant 1
                    final int funct3 = getField(inst, 13, 15, 0);
                    switch (funct3) {
                        case 0b000: { // C.NOP / C.ADDI
                            final int rd = getField(inst, 7, 11, 0);
                            if (rd != 0) { // C.ADDI
                                final int imm = extendSign(getField(inst, 12, 12, 5) |
                                                           getField(inst, 2, 6, 0), 6);
                                x[rd] += imm;
                            } // else: imm != 0 ? HINT : C.NOP

                            pc += 2;
                            break;
                        }
                        case 0b001: { // C.JAL
                            final int offset = extendSign(getField(inst, 12, 12, 11) |
                                                          getField(inst, 11, 11, 4) |
                                                          getField(inst, 9, 10, 8) |
                                                          getField(inst, 8, 8, 10) |
                                                          getField(inst, 7, 7, 6) |
                                                          getField(inst, 6, 6, 7) |
                                                          getField(inst, 3, 5, 1) |
                                                          getField(inst, 2, 2, 5), 12);
                            x[1] = pc + 2;
                            pc += offset;
                            break;
                        }
                        case 0b010: { // C.LI
                            final int rd = getField(inst, 7, 11, 0);
                            if (rd != 0) {
                                final int imm = extendSign(getField(inst, 12, 12, 5) |
                                                           getField(inst, 2, 6, 0), 6);
                                x[rd] = imm;
                            } // else: HINT

                            pc += 2;
                            break;
                        }
                        case 0b011: { // C.ADDI16SP / C.LUI
                            final int rd = getField(inst, 7, 11, 0);
                            if (rd == 2) { // C.ADDI16SP
                                final int imm = extendSign(getField(inst, 12, 12, 9) |
                                                           getField(inst, 6, 6, 4) |
                                                           getField(inst, 5, 5, 6) |
                                                           getField(inst, 3, 4, 7) |
                                                           getField(inst, 2, 2, 5), 10);
                                if (imm == 0) { // Reserved.
                                    throw new R5IllegalInstructionException(inst);
                                }
                                x[2] += imm;
                            } else if (rd != 0) { // C.LUI
                                final int imm = extendSign(getField(inst, 12, 12, 17) |
                                                           getField(inst, 2, 6, 12), 18);
                                if (imm == 0) { // Reserved.
                                    throw new R5IllegalInstructionException(inst);
                                }
                                x[rd] = imm;
                            } // else: HINT

                            pc += 2;
                            break;
                        }
                        case 0b100: { // C.SRLI / C.SRAI / C.ANDI / C.SUB / C.XOR / C.OR / C.AND
                            final int funct2 = getField(inst, 10, 11, 0);
                            final int rd = getField(inst, 7, 9, 0) + 8;
                            switch (funct2) {
                                case 0b00: // C.SRLI
                                case 0b01: { // C.SRAI
                                    final int imm = getField(inst, 12, 12, 5) |
                                                    getField(inst, 2, 6, 0);
                                    // imm[5] = 0 reserved for custom extensions; same as = 1 for us.
                                    if ((funct2 & 0b1) == 0) {
                                        x[rd] = x[rd] >>> imm;
                                    } else {
                                        x[rd] = x[rd] >> imm;
                                    }
                                    break;
                                }
                                case 0b10: { // C.ANDI
                                    final int imm = extendSign(getField(inst, 12, 12, 5) |
                                                               getField(inst, 2, 6, 0), 6);
                                    x[rd] &= imm;
                                    break;
                                }
                                case 0b11: { // C.SUB / C.XOR / C.OR / C.AND
                                    final int funct3b = getField(inst, 5, 6, 0) |
                                                        getField(inst, 12, 12, 2);
                                    final int rs2 = getField(inst, 2, 4, 0) + 8;
                                    switch (funct3b) {
                                        case 0b000: { // C.SUB
                                            x[rd] = x[rd] - x[rs2];
                                            break;
                                        }
                                        case 0b001: { // C.XOR
                                            x[rd] = x[rd] ^ x[rs2];
                                            break;
                                        }
                                        case 0b010: { // C.OR
                                            x[rd] = x[rd] | x[rs2];
                                            break;
                                        }
                                        case 0b011: { // C.AND
                                            x[rd] = x[rd] & x[rs2];
                                            break;
                                        }
                                        // 0b100: C.SUBW
                                        // 0b101: C.ADDW

                                        default: {
                                            throw new R5IllegalInstructionException(inst);
                                        }
                                    }
                                    break;
                                }
                            }

                            pc += 2;
                            break;
                        }
                        case 0b101: { // C.J
                            final int offset = extendSign(getField(inst, 12, 12, 11) |
                                                          getField(inst, 11, 11, 4) |
                                                          getField(inst, 9, 10, 8) |
                                                          getField(inst, 8, 8, 10) |
                                                          getField(inst, 7, 7, 6) |
                                                          getField(inst, 6, 6, 7) |
                                                          getField(inst, 3, 5, 1) |
                                                          getField(inst, 2, 2, 5), 12);
                            pc += offset;
                            break;
                        }
                        case 0b110: // C.BEQZ
                        case 0b111: { // C.BNEZ
                            final int rs1 = getField(inst, 7, 9, 0) + 8;
                            final boolean condition = x[rs1] == 0;
                            if (condition ^ ((funct3 & 0b1) != 0)) {
                                final int offset = extendSign(getField(inst, 12, 12, 8) |
                                                              getField(inst, 10, 11, 3) |
                                                              getField(inst, 5, 6, 6) |
                                                              getField(inst, 3, 4, 1) |
                                                              getField(inst, 2, 2, 5), 9);
                                pc += offset;
                            } else {
                                pc += 2;
                            }
                            break;
                        }

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    break;
                }

                case 0b10: { // Quadrant 2
                    final int funct3 = getField(inst, 13, 15, 0);
                    final int rd = getField(inst, 7, 11, 0);
                    switch (funct3) {
                        case 0b000: { // C.SLLI
                            final int imm = getField(inst, 12, 12, 5) |
                                            getField(inst, 2, 6, 0);
                            // imm[5] = 0 reserved for custom extensions; same as = 1 for us.
                            if (rd != 0) {
                                x[rd] = x[rd] << imm;
                            } // else: HINT

                            pc += 2;
                            break;
                        }
                        // 0b001: C.FLDSP
                        case 0b010: { // C.LWSP
                            if (rd == 0) { // Reserved.
                                throw new R5IllegalInstructionException(inst);
                            }

                            final int offset = getField(inst, 12, 12, 5) |
                                               getField(inst, 4, 6, 2) |
                                               getField(inst, 2, 3, 6);

                            x[rd] = load32(x[2] + offset);

                            pc += 2;
                            break;
                        }
                        // 0b011: C.FLWSP
                        case 0b100: { // C.JR / C.MV / C.EBREAK / C.JALR / C.ADD
                            final int rs2 = getField(inst, 2, 6, 0);
                            if ((inst & (1 << 12)) == 0) { // C.JR / C.MV
                                if (rs2 == 0) { // C.JR
                                    if (rd == 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    pc = x[rd /* = rs1 */] & ~1;
                                } else { // C.MV
                                    if (rd != 0) {
                                        x[rd] = x[rs2];
                                    } // else: HINT

                                    pc += 2;
                                }
                            } else { // C.EBREAK / C.JALR / C.ADD
                                if (rs2 == 0) { // C.EBREAK / C.JALR
                                    if (rd == 0) { // C.EBREAK
                                        throw new R5BreakpointException();
                                    } else { // C.JALR
                                        final int address = x[rd /* = rs1 */] & ~1; // Technically should raise exception on misaligned jump.
                                        final int value = pc + 2; // In case rd == 1, avoid overwriting before using.
                                        x[1] = value;
                                        pc = address;
                                    }
                                } else { // C.ADD
                                    if (rd != 0) {
                                        x[rd] += x[rs2];
                                    } // else: HINT

                                    pc += 2;
                                }
                            }

                            break;
                        }
                        // 0b101: C.FSDSP
                        case 0b110: { // C.SWSP
                            final int offset = getField(inst, 9, 12, 2) |
                                               getField(inst, 7, 8, 6);
                            final int rs2 = getField(inst, 2, 6, 0);
                            final int address = x[2] + offset;
                            final int value = x[rs2];
                            store32(address, value);

                            pc += 2;
                            break;
                        }
                        // 0b111: C.FSWSP

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }

                    break;
                }

                default: {
                    throw new R5IllegalInstructionException(inst);
                }
            }
        }
    }

    private void checkCSR(final int inst, final int csr, final boolean throwIfReadonly) throws R5IllegalInstructionException {
        if (throwIfReadonly && ((csr >= 0xC00 && csr <= 0xC1F) || (csr >= 0xC80 && csr <= 0xC9F)))
            throw new R5IllegalInstructionException(inst);

        // Topmost bits, i.e. csr[11:8], encode access rights for CSR by convention. Of these, the top-most two bits,
        // csr[11:10], encode read-only state, where 0b11: read-only, 0b00..0b10: read-write.
        if (throwIfReadonly && ((csr & 0b1100_0000_0000) == 0b1100_0000_0000))
            throw new R5IllegalInstructionException(inst);
        // The two following bits, csr[9:8], encode the lowest privilege level that can access the CSR.
        if (priv < ((csr >>> 8) & 0b11))
            throw new R5IllegalInstructionException(inst);
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private int readCSR(final int inst, final int csr) throws R5IllegalInstructionException {
        switch (csr) {
            // Floating-Point Control and Status Registers
//            case 0x001: { // fflags, Floating-Point Accrued Exceptions.
//                return fflags;
//            }
//            case 0x002: { // frm, Floating-Point Dynamic Rounding Mode.
//                return frm;
//            }
//            case 0x003: { // fcsr, Floating-Point Control and Status Register (frm + fflags).
//                return (frm << 5) | fflags;
//            }

            // User Trap Setup
            // 0x000: ustatus, User status register.
            // 0x004: uie, User interrupt-enabled register.
            // 0x005: utvec, User trap handler base address.

            // User Trap Handling
            // 0x040: uscratch, Scratch register for user trap handlers.
            // 0x041: uepc, User exception program counter.
            // 0x042: ucause, User trap cause.
            // 0x043: utval, User bad address or instruction.
            // 0x044: uip, User interrupt pending.

            // Supervisor Trap Setup
            case 0x100: { // sstatus, Supervisor status register.
                return getStatus(SSTATUS_MASK);
            }
            // 0x102: sedeleg, Supervisor exception delegation register.
            // 0x103: sideleg, Supervisor interrupt delegation register.
            case 0x104: { // sie, Supervisor interrupt-enable register.
                return mie & mideleg; // Effectively read-only because we don't implement N.
            }
            case 0x105: { // stvec, Supervisor trap handler base address.
                return stvec;
            }
            case 0x106: { // scounteren, Supervisor counter enable.
                return scounteren;
            }

            // Supervisor Trap Handling
            case 0x140: { // sscratch Scratch register for supervisor trap handlers.
                return sscratch;
            }
            case 0x141: { // sepc Supervisor exception program counter.
                return sepc;
            }
            case 0x142: { // scause Supervisor trap cause.
                return scause;
            }
            case 0x143: { // stval Supervisor bad address or instruction.
                return stval;
            }
            case 0x144: { // sip Supervisor interrupt pending.
                return mip & mideleg; // Effectively read-only because we don't implement N.
            }

            // Supervisor Protection and Translation
            case 0x180: { // satp Supervisor address translation and protection.
                if (priv == R5.PRIVILEGE_S && (mstatus & R5.STATUS_TVM_MASK) != 0) {
                    throw new R5IllegalInstructionException(inst);
                }
                return satp;
            }

            // Virtual Supervisor Registers
            // 0x200: vsstatus, Virtual supervisor status register.
            // 0x204: vsie, Virtual supervisor interrupt-enable register.
            // 0x205: vstvec, Virtual supervisor trap handler base address.
            // 0x240: vsscratch, Virtual supervisor scratch register.
            // 0x241: vsepc, Virtual supervisor exception program counter.
            // 0x242: vscause, Virtual supervisor trap cause.
            // 0x243: vstval, Virtual supervisor bad address or instruction.
            // 0x244: vsip, Virtual supervisor interrupt pending.
            // 0x280: vsatp, Virtual supervisor address translation and protection

            // Machine Trap Setup
            case 0x300: { // mstatus Machine status register.
                return getStatus(MSTATUS_MASK);
            }
            case 0x301: { // misa ISA and extensions
                return MISA;
            }
            case 0x302: { // medeleg Machine exception delegation register.
                return medeleg;
            }
            case 0x303: { // mideleg Machine interrupt delegation register.
                return mideleg;
            }
            case 0x304: { // mie Machine interrupt-enable register.
                return mie;
            }
            case 0x305: { // mtvec Machine trap-handler base address.
                return mtvec;
            }
            case 0x306: { // mcounteren Machine counter enable.
                return mcounteren;
            }
            case 0x310: {// mstatush, Additional machine status register, RV32 only.
                return 0; // Hardcoded to zero. MBE = 0, SBE = 0 -> always little-endian.
            }

            // Machine Trap Handling
            case 0x340: { // mscratch Scratch register for machine trap handlers.
                return mscratch;
            }
            case 0x341: { // mepc Machine exception program counter.
                return mepc;
            }
            case 0x342: { // mcause Machine trap cause.
                return mcause;
            }
            case 0x343: { // mtval Machine bad address or instruction.
                return mtval;
            }
            case 0x344: { // mip Machine interrupt pending.
                return mip;
            }
            // 0x34A: mtinst, Machine trap instruction (transformed).
            // 0x34B: mtval2, Machine bad guest physical address.

            // Machine Memory Protection
            // 0x3A0: pmpcfg0. Physical memory protection configuration.
            // 0x3A1: pmpcfg1. Physical memory protection configuration, RV32 only.
            // 0x3A2: pmpcfg2. Physical memory protection configuration.
            // 0x3A3...0x3AE: pmpcfg3...pmpcfg14, Physical memory protection configuration, RV32 only.
            // 0x3AF: pmpcfg15, Physical memory protection configuration, RV32 only.
            // 0x3B0: pmpaddr0, Physical memory protection address register.
            // 0x3B1...0x3EF: pmpaddr1...pmpaddr63, Physical memory protection address register.

            // Hypervisor Trap Setup
            // 0x600: hstatus, Hypervisor status register.
            // 0x602: hedeleg, Hypervisor exception delegation register.
            // 0x603: hideleg, Hypervisor interrupt delegation register.
            // 0x604: hie, Hypervisor interrupt-enable register.
            // 0x606: hcounteren, Hypervisor counter enable.
            // 0x607: hgeie, Hypervisor guest external interrupt-enable register.

            // Hypervisor Trap Handling
            // 0x643: htval, Hypervisor bad guest physical address.
            // 0x644: hip, Hypervisor interrupt pending.
            // 0x645: hvip, Hypervisor virtual interrupt pending.
            // 0x64A: htinst, Hypervisor trap instruction (transformed).
            // 0xE12: hgeip, Hypervisor guest external interrupt pending.

            // Hypervisor Protection and Translation
            // 0x680: hgatp, Hypervisor guest address translation and protection.

            // Hypervisor Counter/Timer Virtualization Registers
            // 0x605: htimedelta, Delta for VS/VU-mode timer.
            // 0x615: htimedeltah, Upper 32 bits of htimedelta, RV32 only.

            //Machine Counter/Timers
            case 0xB00: // mcycle, Machine cycle counter.
            case 0xB02: { // minstret, Machine instructions-retired counter.
                return (int) mcycle;
            }
            //0xB03: mhpmcounter3, Machine performance-monitoring counter.
            //0xB04...0xB1F: mhpmcounter4...mhpmcounter31, Machine performance-monitoring counter.
            case 0xB80: // mcycleh, Upper 32 bits of mcycle, RV32 only.
            case 0xB82: { // minstreth, Upper 32 bits of minstret, RV32 only.
                return (int) (mcycle >> 32);
            }
            //0xB83: mhpmcounter3h, Upper 32 bits of mhpmcounter3, RV32 only.
            //0xB84...0xB9F: mhpmcounter4h...mhpmcounter31h, Upper 32 bits of mhpmcounter4, RV32 only.

            // Counters and Timers
            case 0xC00:  // cycle
            case 0xC02: { // instret
                // See Volume 2 p36: mcounteren/scounteren define availability to next lowest privilege level.
                if (priv < R5.PRIVILEGE_M) {
                    final int counteren;
                    if (priv < R5.PRIVILEGE_S) {
                        counteren = scounteren;
                    } else {
                        counteren = mcounteren;
                    }

                    // counteren[2:0] is IR, TM, CY. As such the bit index matches the masked csr value.
                    if ((counteren & (1 << (csr & 0b11))) == 0) {
                        throw new R5IllegalInstructionException(inst);
                    }
                }
                return (int) mcycle;
            }
            case 0xC01: { // time
                return (int) rtc.getTime();
            }
            // 0xC03 ... 0xC1F: hpmcounter3 ... hpmcounter31
            case 0xC80:  // cycleh
            case 0xC82: { // instreth
                // See Volume 2 p36: mcounteren/scounteren define availability to next lowest privilege level.
                if (priv < R5.PRIVILEGE_M) {
                    final int counteren;
                    if (priv < R5.PRIVILEGE_S) {
                        counteren = scounteren;
                    } else {
                        counteren = mcounteren;
                    }

                    // counteren[2:0] is IR, TM, CY. As such the bit index matches the masked csr value.
                    if ((counteren & (1 << (csr & 0b11))) == 0) {
                        throw new R5IllegalInstructionException(inst);
                    }
                }
                return (int) (mcycle >> 32);
            }
            case 0xC81: { // timeh
                return (int) (rtc.getTime() >>> 32);
            }
            // 0xC83 ... 0xC9F: hpmcounter3h ... hpmcounter31h

            // Machine Information Registers
            case 0xF11: { // mvendorid, Vendor ID.
                return 0; // Not implemented.
            }
            case 0xF12: { // marchid, Architecture ID.
                return 0; // Not implemented.
            }
            case 0xF13: { // mimpid, Implementation ID.
                return 0; // Not implemented.
            }
            case 0xF14: { // mhartid, Hardware thread ID.
                return 0; // Single, primary hart.
            }

            default: {
                throw new R5IllegalInstructionException(inst);
            }
        }
    }

    private void writeCSR(final int inst, final int csr, final int value) throws R5IllegalInstructionException {
        switch (csr) {
            // Floating-Point Control and Status Registers
//            case 0x001: { // fflags, Floating-Point Accrued Exceptions.
//                fflags = (byte) (value & 0b11111);
//            }
//            case 0x002: { // frm, Floating-Point Dynamic Rounding Mode.
//                frm = (byte) (value & 0b111);
//                if (frm >= 5) frm = 0; // TODO Not to spec; should store and raise invalid instruction on FP ops.
//            }
//            case 0x003: { // fcsr, Floating-Point Control and Status Register (frm + fflags).
//                frm = (byte) ((value >>> 5) & 0b111);
//                if (frm >= 5) frm = 0; // TODO Not to spec; should store and raise invalid instruction on FP ops.
//                fflags = (byte) (value & 0b11111);
//                break;
//            }

            // User Trap Setup
            // 0x000: ustatus, User status register.
            // 0x004: uie, User interrupt-enabled register.
            // 0x005: utvec, User trap handler base address.

            // User Trap Handling
            // 0x040: uscratch, Scratch register for user trap handlers.
            // 0x041: uepc, User exception program counter.
            // 0x042: ucause, User trap cause.
            // 0x043: utval, User bad address or instruction.
            // 0x044: uip, User interrupt pending.

            // Supervisor Trap Setup
            case 0x100: { // sstatus, Supervisor status register.
                setStatus((mstatus & ~SSTATUS_MASK) | (value & SSTATUS_MASK));
                break;
            }
            // 0x102: sedeleg, Supervisor exception delegation register.
            // 0x103: sideleg, Supervisor interrupt delegation register.
            case 0x104: { // sie, Supervisor interrupt-enable register.
                final int mask = mideleg; // Can only set stuff that's delegated to S mode.
                mie = (mie & ~mask) | (value & mask);
                break;
            }
            case 0x105: { // stvec, Supervisor trap handler base address.
                stvec = value & ~0b10; // Don't allow reserved modes.
                break;
            }
            case 0x106: { // scounteren, Supervisor counter enable.
                scounteren = value & COUNTEREN_MASK;
                break;
            }

            // Supervisor Trap Handling
            case 0x140: { // sscratch Scratch register for supervisor trap handlers.
                sscratch = value;
                break;
            }
            case 0x141: { // sepc Supervisor exception program counter.
                sepc = value & ~0b1;
                break;
            }
            case 0x142: { // scause Supervisor trap cause.
                scause = value;
                break;
            }
            case 0x143: { // stval Supervisor bad address or instruction.
                stval = value;
                break;
            }
            case 0x144: { // sip Supervisor interrupt pending.
                final int mask = mideleg; // Can only set stuff that's delegated to S mode.
                mip = (mip & ~mask) | (value & mask);
                break;
            }

            // Supervisor Protection and Translation
            case 0x180: { // satp Supervisor address translation and protection.
                final int validatedValue = value & ~R5.SATP_ASID_MASK; // Say no to ASID (not implemented).
                final int change = satp ^ validatedValue;
                if ((change & (R5.SATP_MODE_MASK | R5.SATP_PPN_MASK)) != 0) {
                    if (priv == R5.PRIVILEGE_S && (mstatus & R5.STATUS_TVM_MASK) != 0) {
                        throw new R5IllegalInstructionException(inst);
                    }

                    satp = validatedValue;
                    flushTLB();
                }
                break;
            }

            // Virtual Supervisor Registers
            // 0x200: vsstatus, Virtual supervisor status register.
            // 0x204: vsie, Virtual supervisor interrupt-enable register.
            // 0x205: vstvec, Virtual supervisor trap handler base address.
            // 0x240: vsscratch, Virtual supervisor scratch register.
            // 0x241: vsepc, Virtual supervisor exception program counter.
            // 0x242: vscause, Virtual supervisor trap cause.
            // 0x243: vstval, Virtual supervisor bad address or instruction.
            // 0x244: vsip, Virtual supervisor interrupt pending.
            // 0x280: vsatp, Virtual supervisor address translation and protection

            // Machine Trap Setup
            case 0x300: { // mstatus Machine status register.
                setStatus(value);
                break;
            }
            case 0x301: { // misa ISA and extensions
                break; // We do not support changing feature sets dynamically.
            }
            case 0x302: { // medeleg Machine exception delegation register.
                // From Volume 2 p31: For exceptions that cannot occur in less privileged modes, the corresponding
                // medeleg bits should be hardwired to zero. In particular, medeleg[11] is hardwired to zero.
                medeleg = value & ~(1 << R5.EXCEPTION_MACHINE_ECALL);
                break;
            }
            case 0x303: { // mideleg Machine interrupt delegation register.
                final int mask = R5.SSIP_MASK | R5.STIP_MASK | R5.SEIP_MASK;
                mideleg = (mideleg & ~mask) | (value & mask);
                break;
            }
            case 0x304: { // mie Machine interrupt-enable register.
                final int mask = R5.MEIP_MASK | R5.MTIP_MASK | R5.MSIP_MASK | R5.SEIP_MASK | R5.STIP_MASK | R5.SSIP_MASK;
                mie = (mie & ~mask) | (value & mask);
                break;
            }
            case 0x305: { // mtvec Machine trap-handler base address.
                mtvec = value & ~0b10; // Don't allow reserved modes.
            }
            case 0x306: { // mcounteren Machine counter enable.
                mcounteren = value & COUNTEREN_MASK;
                break;
            }
            // 0x310: mstatush, Additional machine status register, RV32 only.

            // Machine Trap Handling
            case 0x340: { // mscratch Scratch register for machine trap handlers.
                mscratch = value;
                break;
            }
            case 0x341: { // mepc Machine exception program counter.
                mepc = value & ~0b1; // p38: Lowest bit must always be zero.
                break;
            }
            case 0x342: { // mcause Machine trap cause.
                mcause = value;
                break;
            }
            case 0x343: { // mtval Machine bad address or instruction.
                mtval = value;
                break;
            }
            case 0x344: { // mip Machine interrupt pending.
                // p32: MEIP, MTIP, MSIP are readonly in mip.
                final int mask = R5.SEIP_MASK | R5.STIP_MASK | R5.SSIP_MASK;
                mip = (mip & ~mask) | (value & mask);
                break;
            }
            // 0x34A: mtinst, Machine trap instruction (transformed).
            // 0x34B: mtval2, Machine bad guest physical address.

            // Machine Memory Protection
            // 0x3A0: pmpcfg0. Physical memory protection configuration.
            // 0x3A1: pmpcfg1. Physical memory protection configuration, RV32 only.
            // 0x3A2: pmpcfg2. Physical memory protection configuration.
            // 0x3A3...0x3AE: pmpcfg3...pmpcfg14, Physical memory protection configuration, RV32 only.
            // 0x3AF: pmpcfg15, Physical memory protection configuration, RV32 only.
            // 0x3B0: pmpaddr0, Physical memory protection address register.
            // 0x3B1...0x3EF: pmpaddr1...pmpaddr63, Physical memory protection address register.

            // Hypervisor Trap Setup
            // 0x600: hstatus, Hypervisor status register.
            // 0x602: hedeleg, Hypervisor exception delegation register.
            // 0x603: hideleg, Hypervisor interrupt delegation register.
            // 0x604: hie, Hypervisor interrupt-enable register.
            // 0x606: hcounteren, Hypervisor counter enable.
            // 0x607: hgeie, Hypervisor guest external interrupt-enable register.

            // Hypervisor Trap Handling
            // 0x643: htval, Hypervisor bad guest physical address.
            // 0x644: hip, Hypervisor interrupt pending.
            // 0x645: hvip, Hypervisor virtual interrupt pending.
            // 0x64A: htinst, Hypervisor trap instruction (transformed).

            // Hypervisor Protection and Translation
            // 0x680: hgatp, Hypervisor guest address translation and protection.

            // Hypervisor Counter/Timer Virtualization Registers
            // 0x605: htimedelta, Delta for VS/VU-mode timer.
            // 0x615: htimedeltah, Upper 32 bits of htimedelta, RV32 only.

            default: {
                throw new R5IllegalInstructionException(inst);
            }
        }
    }

    private int getStatus(final int mask) {
        final int value = (mstatus | (fs << R5.STATUS_FS_SHIFT)) & mask;
        if (((value & R5.STATUS_FS_MASK) == R5.STATUS_FS_MASK) || ((value & R5.STATUS_XS_MASK) == R5.STATUS_XS_MASK)) {
            return value | R5.STATUS_SD_MASK;
        }
        return value;
    }

    private void setStatus(final int value) {
        final int change = mstatus ^ value;
        final boolean mmuConfigChanged =
                (change & (R5.STATUS_MPRV_MASK | R5.STATUS_SUM_MASK | R5.STATUS_MXR_MASK)) != 0 ||
                ((mstatus & R5.STATUS_MPRV_MASK) != 0 && (change & R5.STATUS_MPP_MASK) != 0);
        if (mmuConfigChanged) {
            flushTLB();
        }

        fs = (byte) ((value & R5.STATUS_FS_MASK) >>> R5.STATUS_FS_SHIFT);

        final int mask = MSTATUS_MASK & ~R5.STATUS_FS_MASK;
        mstatus = (mstatus & ~mask) | (value & mask);
    }

    private void setPrivilege(final int level) {
        if (priv == level) {
            return;
        }

        flushTLB();

        priv = level;
    }

    private void raiseException(final int cause, final int value) {
        // Exceptions take cycle.
        mcycle++;

        // Check whether to run supervisor level trap instead of machine level one.
        // We don't implement the N extension (user level interrupts) so if we're
        // currently in S or U privilege level we'll run the S level trap handler
        // either way -- assuming that the current interrupt/exception is allowed
        // to be delegated by M level.
        final boolean runInSupervisorMode;
        final int exception = cause & ~R5.INTERRUPT;
        if (priv <= R5.PRIVILEGE_S) {
            if (cause != exception) { // Interrupt.
                runInSupervisorMode = ((mideleg >>> exception) & 0b1) != 0;
            } else {
                runInSupervisorMode = ((medeleg >>> exception) & 0b1) != 0;
            }
        } else {
            runInSupervisorMode = false;
        }

        // Was interrupt for current priv level enabled? There are cases we can
        // get here even for interrupts! Specifically when an M level interrupt
        // is raised while in S mode. This will get here even if M level interrupt
        // enabled bit is zero, as per spec (Volume 2 p21).
        final int oldIE = (mstatus >>> priv) & 0b1;

        final int vec;
        if (runInSupervisorMode) {
            scause = cause;
            sepc = pc;
            stval = value;
            mstatus = (mstatus & ~R5.STATUS_SPIE_MASK) |
                      (oldIE << R5.STATUS_SPIE_SHIFT);
            mstatus = (mstatus & ~R5.STATUS_SPP_MASK) |
                      (priv << R5.STATUS_SPP_SHIFT);
            mstatus &= ~R5.STATUS_SIE_MASK;
            setPrivilege(R5.PRIVILEGE_S);
            vec = stvec;
        } else {
            mcause = cause;
            mepc = pc;
            mtval = value;
            mstatus = (mstatus & ~R5.STATUS_MPIE_MASK) |
                      (oldIE << R5.STATUS_MPIE_SHIFT);
            mstatus = (mstatus & ~R5.STATUS_MPP_MASK) |
                      (priv << R5.STATUS_MPP_SHIFT);
            mstatus &= ~R5.STATUS_MIE_MASK;
            setPrivilege(R5.PRIVILEGE_M);
            vec = mtvec;
        }

        final int mode = vec & 0b11;
        switch (mode) {
            case 0b01: { // Vectored
                if ((cause & R5.INTERRUPT) != 0) {
                    pc = vec + 4 * exception;
                } else {
                    pc = vec & ~0b1;
                }
                break;
            }
            case 0b00: // Direct
            default: {
                pc = vec;
                break;
            }
        }
    }

    private void mret() {
        final int mpp = (mstatus & R5.STATUS_MPP_MASK) >>> R5.STATUS_MPP_SHIFT; // Previous privilege level.
        final int mpie = (mstatus & R5.STATUS_MPIE_MASK) >>> R5.STATUS_MPIE_SHIFT; // Preview interrupt-enable state.
        mstatus = (mstatus & ~(1 << mpp)) |
                  (mpie << mpp);
        mstatus |= R5.STATUS_MPIE_MASK;
        mstatus &= ~R5.STATUS_MPP_MASK;
        setPrivilege(mpp);
        pc = mepc;
    }

    private void sret() {
        final int spp = (mstatus & R5.STATUS_SPP_MASK) >>> R5.STATUS_SPP_SHIFT; // Previous privilege level.
        final int spie = (mstatus & R5.STATUS_SPIE_MASK) >>> R5.STATUS_SPIE_SHIFT; // Preview interrupt-enable state.
        mstatus = (mstatus & ~(1 << spp)) |
                  (spie << spp);
        mstatus |= R5.STATUS_SPIE_MASK;
        mstatus &= ~R5.STATUS_SPP_MASK;
        setPrivilege(spp);
        pc = sepc;
    }

    private void raiseException(final int cause) {
        raiseException(cause, 0);
    }

    private boolean raiseInterrupt() {
        int pending = mip & mie;
        assert pending != 0;

        int enabled = 0;
        switch (priv) {
            case R5.PRIVILEGE_M: {
                // Check global MIE flag.
                if ((mstatus & R5.STATUS_MIE_MASK) != 0)
                    enabled = ~mideleg;
                break;
            }
            case R5.PRIVILEGE_S: {
                // V2p21: interrupts handled by a higher privilege level, i.e. that are not delegated
                // to a lower privilege level, will always fire -- even if their global flag is false!
                enabled = ~mideleg;

                // Check global SIE flag.
                if ((mstatus & R5.STATUS_SIE_MASK) != 0)
                    enabled |= mideleg;
                break;
            }
            // We don't have the "N" extension for user-level interrupts, so all our interrupts will
            // always be of M or S level, hence always enabled while in U mode (V2p21).
            case R5.PRIVILEGE_U:
            default: {
                enabled = 0b1111_1111_1111_1111_1111_1111_1111_1111;
                break;
            }
        }

        pending = pending & enabled;
        if (pending == 0) {
            return false;
        }

        // p33: Interrupt order is handled in decreasing order of privilege mode, and inside a single
        // privilege mode in order E,S,T.
        // TODO custom interrupts have highest prio and are processed low to high
        if ((pending & R5.MEIP_MASK) != 0) {
            raiseException(R5.MEIP_SHIFT | R5.INTERRUPT);
        } else if ((pending & R5.MSIP_MASK) != 0) {
            raiseException(R5.MSIP_SHIFT | R5.INTERRUPT);
        } else if ((pending & R5.MTIP_MASK) != 0) {
            raiseException(R5.MTIP_SHIFT | R5.INTERRUPT);
        } else if ((pending & R5.SEIP_MASK) != 0) {
            raiseException(R5.SEIP_SHIFT | R5.INTERRUPT);
        } else if ((pending & R5.SSIP_MASK) != 0) {
            raiseException(R5.SSIP_SHIFT | R5.INTERRUPT);
        } else if ((pending & R5.STIP_MASK) != 0) {
            raiseException(R5.STIP_SHIFT | R5.INTERRUPT);
        } else {
            return false; // We don't support custom interrupts for now.
        }

        return true;
    }

    private byte load8(final int address) throws MemoryAccessException {
        return (byte) load(address, Sizes.SIZE_8, Sizes.SIZE_8_LOG2);
    }

    private void store8(final int address, final byte value) throws MemoryAccessException {
        store(address, value, Sizes.SIZE_8, Sizes.SIZE_8_LOG2);
    }

    private short load16(final int address) throws MemoryAccessException {
        return (short) load(address, Sizes.SIZE_16, Sizes.SIZE_16_LOG2);
    }

    private void store16(final int address, final short value) throws MemoryAccessException {
        store(address, value, Sizes.SIZE_16, Sizes.SIZE_16_LOG2);
    }

    private int load32(final int address) throws MemoryAccessException {
        return load(address, Sizes.SIZE_32, Sizes.SIZE_32_LOG2);
    }

    private void store32(final int address, final int value) throws MemoryAccessException {
        store(address, value, Sizes.SIZE_32, Sizes.SIZE_32_LOG2);
    }

    private int fetch(final int address) throws MemoryAccessException {
        if ((address & 1) != 0) {
            throw new MisalignedFetchException(address);
        }

        final int tlbIndex = (address >>> R5.PAGE_ADDRESS_SHIFT) & (TLB_SIZE - 1);
        final int virtualAddress = address & ~R5.PAGE_ADDRESS_MASK;
        final TLBEntry entry = tlb_fetch[tlbIndex];
        if (entry.virtualAddress == virtualAddress) {
            return entry.memory.getInt(address + entry.toLocal);
        } else {
            return fetchSlow(address);
        }
    }

    private int load(final int address, final int size, final int sizeLog2) throws MemoryAccessException {
        final int tlbIndex = (address >>> R5.PAGE_ADDRESS_SHIFT) & (TLB_SIZE - 1);
        final int alignment = size / 8; // Enforce aligned memory access.
        final int alignmentMask = alignment - 1;
        final int virtualAddress = address & ~(R5.PAGE_ADDRESS_MASK & ~alignmentMask);
        final TLBEntry entry = tlb_read[tlbIndex];
        if (entry.virtualAddress == virtualAddress) {
            return entry.device.load(address + entry.toOffset, sizeLog2);
        } else {
            return loadSlow(address, sizeLog2);
        }
    }

    private void store(final int address, final int value, final int size, final int sizeLog2) throws MemoryAccessException {
        final int tlbIndex = (address >>> R5.PAGE_ADDRESS_SHIFT) & (TLB_SIZE - 1);
        final int alignment = size / 8; // Enforce aligned memory access.
        final int alignmentMask = alignment - 1;
        final int virtualAddress = address & ~(R5.PAGE_ADDRESS_MASK & ~alignmentMask);
        final TLBEntry entry = tlb_write[tlbIndex];
        if (entry.virtualAddress == virtualAddress) {
            entry.device.store(address + entry.toOffset, value, sizeLog2);
        } else {
            storeSlow(address, value, sizeLog2);
        }
    }

    private int fetchSlow(final int address) throws MemoryAccessException {
        final int physicalAddress = getPhysicalAddress(address, AccessType.FETCH);
        final MemoryRange range = physicalMemory.getMemoryRange(physicalAddress);
        if (range == null || !(range.device instanceof PhysicalMemory)) {
            throw new FetchFaultException(address);
        }

        final TLBEntry entry = updateTLB(tlb_fetch, address, physicalAddress, range);
        return entry.memory.getInt(address + entry.toLocal);
    }

    private int loadSlow(final int address, final int sizeLog2) throws MemoryAccessException {
        final int size = 1 << sizeLog2;
        final int alignment = address & (size - 1);
        if (alignment != 0) {
            throw new MisalignedLoadException(address);
        } else {
            final int physicalAddress = getPhysicalAddress(address, AccessType.READ);
            final MemoryRange range = physicalMemory.getMemoryRange(physicalAddress);
            if (range == null) {
                LOGGER.debug("Trying to load from invalid physical address [{}].", address);
                return 0;
            } else if (range.device instanceof PhysicalMemory) {
                final TLBEntry entry = updateTLB(tlb_read, address, physicalAddress, range);
                return entry.device.load(address + entry.toOffset, sizeLog2);
            } else {
                return range.device.load(physicalAddress - range.start, sizeLog2);
            }
        }
    }

    private void storeSlow(final int address, final int value, final int sizeLog2) throws MemoryAccessException {
        final int size = 1 << sizeLog2;
        final int alignment = address & (size - 1);
        if (alignment != 0) {
            throw new MisalignedStoreException(address);
        } else {
            final int physicalAddress = getPhysicalAddress(address, AccessType.WRITE);
            final MemoryRange range = physicalMemory.getMemoryRange(physicalAddress);
            if (range == null) {
                LOGGER.debug("Trying to store to invalid physical address [{}].", address);
            } else if (range.device instanceof PhysicalMemory) {
                final TLBEntry entry = updateTLB(tlb_write, address, physicalAddress, range);
                final int offset = address + entry.toOffset;
                entry.device.store(offset, value, sizeLog2);
                physicalMemory.setDirty(range, offset);
            } else {
                range.device.store(physicalAddress - range.start, value, sizeLog2);
            }
        }
    }

    private int getPhysicalAddress(final int virtualAddress, final AccessType accessType) throws MemoryAccessException {
        final int privilege;
        if ((mstatus & R5.STATUS_MPRV_MASK) != 0 && accessType != AccessType.FETCH) {
            privilege = (mstatus & R5.STATUS_MPP_MASK) >>> R5.STATUS_MPP_SHIFT;
        } else {
            privilege = this.priv;
        }

        if (privilege == R5.PRIVILEGE_M) {
            return virtualAddress;
        }

        if ((satp & R5.SATP_MODE_MASK) == 0) {
            return virtualAddress;
        }

        // Virtual address structure:  VPN1[31:22], VPN0[21:12], page offset[11:0]
        // Physical address structure: PPN1[33:22], PPN0[21:12], page offset[11:0]
        // Page table entry structure: PPN1[31:20], PPN0[19:10], RSW[9:8], D, A, G, U, X, W, R, V

        // Virtual address translation, V2p75f.
        int pteAddress = (satp & R5.SATP_PPN_MASK) << R5.PAGE_ADDRESS_SHIFT; // 1.
        for (int i = R5.SV32_LEVELS - 1; i >= 0; i--) { // 2.
            final int vpnShift = R5.PAGE_ADDRESS_SHIFT + R5.SV32_XPN_SIZE * i;
            final int vpn = (virtualAddress >>> vpnShift) & R5.SV32_XPN_MASK;
            pteAddress += vpn << R5.SV32_PTE_SIZE_LOG2; // equivalent to vpn * PTE size
            int pte = physicalMemory.load(pteAddress, 2); // 3.

            if ((pte & R5.PTE_V_MASK) == 0 || ((pte & R5.PTE_R_MASK) == 0 && (pte & R5.PTE_W_MASK) != 0)) { // 4.
                throw getPageFaultException(accessType, virtualAddress);
            }

            int xwr = pte & (R5.PTE_X_MASK | R5.PTE_W_MASK | R5.PTE_R_MASK);
            if (xwr == 0) { // 5.
                final int ppn = pte >>> R5.PTE_DATA_BITS;
                pteAddress = ppn << R5.PAGE_ADDRESS_SHIFT;
                continue;
            }

            // 6. Leaf node, do access permission checks.

            // Check reserved/invalid configurations.
            if ((xwr & R5.PTE_R_MASK) == 0 && (xwr & R5.PTE_W_MASK) != 0) {
                throw getPageFaultException(accessType, virtualAddress);
            }

            // Check privilege. Can only be in S or U mode here, M was handled above. V2p61.
            final int userModeFlag = pte & R5.PTE_U_MASK;
            if (privilege == R5.PRIVILEGE_S) {
                if (userModeFlag != 0 &&
                    (accessType == AccessType.FETCH || (mstatus & R5.STATUS_SUM_MASK) == 0))
                    throw getPageFaultException(accessType, virtualAddress);
            } else if (userModeFlag == 0) {
                throw getPageFaultException(accessType, virtualAddress);
            }

            // MXR allows read on execute-only pages.
            if ((mstatus & R5.STATUS_MXR_MASK) != 0) {
                xwr |= R5.PTE_R_MASK;
            }

            // Check access flags.
            if ((xwr & accessType.mask) == 0) {
                throw getPageFaultException(accessType, virtualAddress);
            }

            // 8. Update accessed and dirty flags.
            if ((pte & R5.PTE_A_MASK) == 0 ||
                (accessType == AccessType.WRITE && (pte & R5.PTE_D_MASK) == 0)) {
                pte |= R5.PTE_A_MASK;
                if (accessType == AccessType.WRITE) {
                    pte |= R5.PTE_D_MASK;
                }

                physicalMemory.store(pteAddress, pte, 2);
            }

            // 9. physical address = pte.ppn[LEVELS-1:i], va.vpn[i-1:0], va.pgoff
            final int vpnAndPageOffsetMask = (1 << vpnShift) - 1;
            final int ppn = (pte >>> R5.PTE_DATA_BITS) << R5.PAGE_ADDRESS_SHIFT;
            return (ppn & ~vpnAndPageOffsetMask) | (virtualAddress & vpnAndPageOffsetMask);
        }

        throw getPageFaultException(accessType, virtualAddress);
    }

    private static MemoryAccessException getPageFaultException(final AccessType accessType, final int address) {
        switch (accessType) {
            case READ:
                return new LoadPageFaultException(address);
            case WRITE:
                return new StorePageFaultException(address);
            case FETCH:
                return new FetchPageFaultException(address);
            default:
                throw new AssertionError();
        }
    }

    private static TLBEntry updateTLB(final TLBEntry[] tlb, final int address, final int physicalAddress, final MemoryRange range) {
        final int tlbIndex = (address >>> R5.PAGE_ADDRESS_SHIFT) & (TLB_SIZE - 1);
        final int virtualAddress = address & ~R5.PAGE_ADDRESS_MASK;

        final TLBEntry entry = tlb[tlbIndex];
        entry.virtualAddress = virtualAddress;
        entry.toOffset = physicalAddress - address - range.start;
        entry.device = range.device;

        return entry;
    }

    private void flushTLB() {
        // Only reset the most necessary field, the virtual address (which we use to check if an entry is applicable).
        // Reset per-array for *much* faster clears due to it being a faster memory access pattern/the JIT being able
        // to more efficiently optimize it (probably the latter, expect this gets replaced by a memset).
        for (int i = 0; i < TLB_SIZE; i++) {
            tlb_fetch[i].virtualAddress = -1;
        }
        for (int i = 0; i < TLB_SIZE; i++) {
            tlb_read[i].virtualAddress = -1;
        }
        for (int i = 0; i < TLB_SIZE; i++) {
            tlb_write[i].virtualAddress = -1;
        }
    }

    private void flushTLB(final int address) {
        flushTLB();
    }

    /**
     * Extract a field encoded in an integer value and shifts it to the desired output position.
     * The length is determined by the destination low and high bit indices.
     *
     * @param value       the value that contains the field.
     * @param srcBitFrom  the lowest bit of the field in the source value.
     * @param srcBitUntil the highest bit of the field in the source value.
     * @param destBit     the lowest bit of the field in the destination (returned) value.
     * @return the extracted field at the bit location specified in <code>destBitFrom</code> and <code>destBitUntil</code>.
     */
    private static int getField(final int value, final int srcBitFrom, final int srcBitUntil, final int destBit) {
        // For bit-shifts Java always only uses the lowest five bits for the right-hand operand,
        // so we can't be clever and shift by a negative amount; need to branch here.
        // NB: This method is optimized for bytecode size to make sure it gets inlined.
        return (destBit >= srcBitFrom
                ? value << (destBit - srcBitFrom)
                : value >>> (srcBitFrom - destBit))
               & ((1 << (srcBitUntil - srcBitFrom + 1)) - 1) << destBit;
    }

    private static int extendSign(final int value, final int width) {
        return (value << (32 - width)) >> (32 - width);
    }

    private enum AccessType {
        READ(R5.PTE_R_MASK),
        WRITE(R5.PTE_W_MASK),
        FETCH(R5.PTE_X_MASK),
        ;

        public final int mask;

        AccessType(final int mask) {
            this.mask = mask;
        }
    }

    private static final class TLBEntry {
        public int virtualAddress = -1;
        public int toOffset;
        public MemoryMappedDevice device;
    }

    public R5CPUStateSnapshot getState() {
        final R5CPUStateSnapshot state = new R5CPUStateSnapshot();

        state.pc = pc;
        System.arraycopy(x, 0, state.x, 0, 32);

//        System.arraycopy(f, 0, state.f, 0, 32);
//        state.fflags = fflags;
//        state.frm = frm;

        state.reservation_set = reservation_set;

        state.mcycle = mcycle;

        state.mstatus = mstatus;
        state.mtvec = mtvec;
        state.medeleg = medeleg;
        state.mideleg = mideleg;
        state.mip = mip;
        state.mie = mie;
        state.mcounteren = mcounteren;
        state.mscratch = mscratch;
        state.mepc = mepc;
        state.mcause = mcause;
        state.mtval = mtval;
        state.fs = fs;

        state.stvec = stvec;
        state.scounteren = scounteren;
        state.sscratch = sscratch;
        state.sepc = sepc;
        state.scause = scause;
        state.stval = stval;
        state.satp = satp;

        state.priv = priv;
        state.waitingForInterrupt = waitingForInterrupt;

        return state;
    }

    public void setState(final R5CPUStateSnapshot state) {
        pc = state.pc;
        System.arraycopy(state.x, 0, x, 0, 32);

//        System.arraycopy(state.f, 0, f, 0, 32);
//        fflags = state.fflags;
//        frm = state.frm;

        reservation_set = state.reservation_set;

        mcycle = state.mcycle;

        mstatus = state.mstatus;
        mtvec = state.mtvec;
        medeleg = state.medeleg;
        mideleg = state.mideleg;
        mip = state.mip;
        mie = state.mie;
        mcounteren = state.mcounteren;
        mscratch = state.mscratch;
        mepc = state.mepc;
        mcause = state.mcause;
        mtval = state.mtval;
        fs = state.fs;

        stvec = state.stvec;
        scounteren = state.scounteren;
        sscratch = state.sscratch;
        sepc = state.sepc;
        scause = state.scause;
        stval = state.stval;
        satp = state.satp;

        priv = state.priv;
        waitingForInterrupt = state.waitingForInterrupt;
    }
}
