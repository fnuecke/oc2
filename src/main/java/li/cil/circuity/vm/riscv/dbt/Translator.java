package li.cil.circuity.vm.riscv.dbt;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.BitUtils;
import li.cil.circuity.vm.UnsafeGetter;
import li.cil.circuity.vm.riscv.R5CPU;
import li.cil.circuity.vm.riscv.exception.R5BreakpointException;
import li.cil.circuity.vm.riscv.exception.R5ECallException;
import li.cil.circuity.vm.riscv.exception.R5Exception;
import li.cil.circuity.vm.riscv.exception.R5IllegalInstructionException;
import org.apache.commons.lang3.ClassUtils;
import org.objectweb.asm.*;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class Translator {
    private static final Unsafe UNSAFE = UnsafeGetter.get();

    // Threshold of instructions to emit to generate class. There's a point where they
    // can be too small to justify the overhead of calling them (since they have to be
    // called via INVOKEVIRTUAL).
    private static final int MIN_INSTRUCTIONS = 2;//9;

    private static final Type CPU_TYPE = Type.getType(R5CPU.class);
    private static final Type TRACE_TYPE = Type.getType(Trace.class);
    private static final Type ECALL_EXCEPTION_TYPE = Type.getType(R5ECallException.class);
    private static final Type BREAKPOINT_EXCEPTION_TYPE = Type.getType(R5BreakpointException.class);
    private static final Type ILLEGAL_INSTRUCTION_EXCEPTION_TYPE = Type.getType(R5IllegalInstructionException.class);

    private static final org.objectweb.asm.commons.Method INIT_VOID = org.objectweb.asm.commons.Method.getMethod("void <init> ()");
    private static final org.objectweb.asm.commons.Method INIT_INT = org.objectweb.asm.commons.Method.getMethod("void <init> (int)");

    private static final String CPU_FIELD_NAME = "cpu";

    // First argument to the method, the reference to the CPU we're working on.
    private static final int CPU_LOCAL_INDEX = 1; // R5CPU ref, length = 1

    // On-demand updated local holding current actual PC.
    // Used to bake instOffset for pc fixup on runtime exceptions.
    private static final int PC_LOCAL_INDEX = 2; // int, length = 1

    // Local for holding current cycles. Saves the GETFIELD for each increment.
    private static final int MCYCLE_LOCAL_INDEX = 3; // long, length = 2

    // We remap locals of inlined methods to start here.
    private static final int INLINED_LOCALS_START = 5;

    // Cached opcode implementations by name for faster lookup in generation.
    private static final Map<String, OpcodeMethod> OPCODE_METHODS = new HashMap<>();

    private final TranslatorJob request;
    private final String className;
    private final MethodVisitor mv;
    private int instOffset;
    int emittedInstructions;

    private Translator(final TranslatorJob request, final String className, final MethodVisitor mv) {
        this.request = request;
        this.className = className;
        this.mv = mv;
    }

    @Nullable
    public static Trace translateTrace(final TranslatorJob request) {
        final String className = TRACE_TYPE.getInternalName() + "$" + Integer.toHexString(request.pc);

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
                className,
                null,
                TRACE_TYPE.getInternalName(),
                null);

        cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, CPU_FIELD_NAME, CPU_TYPE.getDescriptor(), null, null);

        generateConstructor(cw, className);
        if (!generateExecuteMethod(cw, className, request)) {
            return null;
        }

        cw.visitEnd();

        return instantiateTrace(defineClass(cw.toByteArray()), request.cpu);
    }

    private static Class<Trace> defineClass(final byte[] data) {
        @SuppressWarnings("unchecked") final Class<Trace> traceClass = (Class<Trace>) UNSAFE.defineAnonymousClass(R5CPU.class, data, null);
        UNSAFE.ensureClassInitialized(traceClass);
        return traceClass;
    }

    private static Trace instantiateTrace(final Class<Trace> traceClass, final R5CPU cpu) {
        try {
            return traceClass.getDeclaredConstructor(R5CPU.class).newInstance(cpu);
        } catch (final Throwable e) {
            throw new AssertionError("Failed instantiating trace.", e);
        }
    }

    private static void generateConstructor(final ClassVisitor cv, final String className) {
        final MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, CPU_TYPE), null, null);

        mv.visitCode();

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, TRACE_TYPE.getInternalName(), INIT_VOID.getName(), INIT_VOID.getDescriptor(), false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, CPU_FIELD_NAME, CPU_TYPE.getDescriptor());
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private static boolean generateExecuteMethod(final ClassVisitor cv, final String className, final TranslatorJob request) {
        final MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "execute", "()V", null, new String[]{
                Type.getInternalName(R5Exception.class),
                Type.getInternalName(MemoryAccessException.class)
        });

        mv.visitCode();

        final Translator translator = new Translator(request, className, mv);
        translator.translateTrace();
        if (translator.emittedInstructions < MIN_INSTRUCTIONS) {
            return false;
        }

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
        return true;
    }

    private void translateTrace() {
        final Label startLabel = new Label();
        final Label returnLabel = new Label();
        final Label catchLabel = new Label();
        final Label endLabel = new Label();

        mv.visitLocalVariable("cpu", CPU_TYPE.getDescriptor(), null, startLabel, endLabel, CPU_LOCAL_INDEX);
        mv.visitLocalVariable("currentPC", Type.INT_TYPE.getDescriptor(), null, startLabel, endLabel, PC_LOCAL_INDEX);
        mv.visitLocalVariable("mcycle", Type.LONG_TYPE.getDescriptor(), null, startLabel, returnLabel, MCYCLE_LOCAL_INDEX);

        mv.visitTryCatchBlock(startLabel, returnLabel, catchLabel, null);

        generateCPULocal();
        generatePCLocal();
        generateMcycleLocal();

        mv.visitLabel(startLabel);

        try { // Catch illegal instruction exceptions to generate final throw instruction.

            instOffset = request.instOffset;
            final int instEnd = request.instEnd;
            int inst = request.firstInst;

            // TODO trim nops completely, i.e. anything where rd = 0 that just computes and writes to it
            // TODO test if incrementing instOffset/pc in generated method is better than generating a ton of constants
            // TODO maybe inline amo32
            // TODO maybe inline store/load
            // TODO maybe inline... everything

            for (; ; ) { // End of page check at the bottom since we enter with a valid inst.
                emittedInstructions++;

                incCycle();

                if ((inst & 0b11) == 0b11) {
                    final int opcode = BitUtils.getField(inst, 0, 6, 0);
                    final int rd = BitUtils.getField(inst, 7, 11, 0);
                    final int rs1 = BitUtils.getField(inst, 15, 19, 0);

                    switch (opcode) {
                        case 0b0010011: {
                            final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                            final int imm = inst >> 20;
                            switch (funct3) {
                                case 0b000: { // ADDI
                                    invokeOp("addi", rd, rs1, imm);
                                    break;
                                }
                                case 0b010: { // SLTI
                                    invokeOp("slti", rd, rs1, imm);
                                    break;
                                }
                                case 0b011: { // SLTIU
                                    invokeOp("sltiu", rd, rs1, imm);
                                    break;
                                }
                                case 0b100: { // XORI
                                    invokeOp("xori", rd, rs1, imm);
                                    break;
                                }
                                case 0b110: { // ORI
                                    invokeOp("ori", rd, rs1, imm);
                                    break;
                                }
                                case 0b111: { // ANDI
                                    invokeOp("andi", rd, rs1, imm);
                                    break;
                                }
                                case 0b001: { // SLLI
                                    if ((inst & 0b1111111_00000_00000_000_00000_0000000) != 0) {
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    invokeOp("slli", rd, rs1, imm);
                                    break;
                                }
                                case 0b101: {
                                    final int funct7 = BitUtils.getField(imm, 5, 11, 0);
                                    switch (funct7) {
                                        case 0b0000000: { // SRLI
                                            invokeOp("srli", rd, rs1, imm);
                                            break;
                                        }
                                        case 0b0100000: { // SRAI
                                            invokeOp("srai", rd, rs1, imm);
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

                            instOffset += 4;
                            break;
                        }

                        case 0b0110111: { // LUI
                            invokeOp("lui", inst, rd);

                            instOffset += 4;
                            break;
                        }

                        case 0b0010111: { // AUIPC
                            invokeOp("auipc", inst, rd, instOffset + request.toPC);

                            instOffset += 4;
                            break;
                        }

                        case 0b0110011: {
                            final int rs2 = BitUtils.getField(inst, 20, 24, 0);
                            final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                            final int funct7 = BitUtils.getField(inst, 25, 31, 0);
                            switch (funct7) {
                                case 0b000001: {
                                    switch (funct3) {
                                        case 0b000: { // MUL
                                            invokeOp("mul", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b001: { // MULH
                                            invokeOp("mulh", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b010: { // MULHSU
                                            invokeOp("mulhsu", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b011: { // MULHU
                                            invokeOp("mulhu", rd, rs1, rs2);
                                            break;
                                        }

                                        case 0b100: { // DIV
                                            invokeOp("div", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b101: { // DIVU
                                            invokeOp("divu", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b110: { // REM
                                            invokeOp("rem", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b111: { // REMU
                                            invokeOp("remu", rd, rs1, rs2);
                                            break;
                                        }

                                        default: {
                                            throw new R5IllegalInstructionException(inst);
                                        }
                                    }

                                    break;
                                }
                                case 0b0000000:
                                case 0b0100000: {
                                    switch (funct3 | funct7) {
                                        //noinspection PointlessBitwiseExpression
                                        case 0b000 | 0b0000000: { // ADD
                                            invokeOp("add", rd, rs1, rs2);
                                            break;
                                        }
                                        //noinspection PointlessBitwiseExpression
                                        case 0b000 | 0b0100000: { // SUB
                                            invokeOp("sub", rd, rs1, rs2);

                                            break;
                                        }
                                        case 0b001: { // SLL
                                            invokeOp("sll", rd, rs1, rs2);

                                            break;
                                        }
                                        case 0b010: { // SLT
                                            invokeOp("slt", rd, rs1, rs2);

                                            break;
                                        }
                                        case 0b011: { // SLTU
                                            invokeOp("sltu", rd, rs1, rs2);

                                            break;
                                        }
                                        case 0b100: { // XOR
                                            invokeOp("xor", rd, rs1, rs2);

                                            break;
                                        }
                                        //noinspection PointlessBitwiseExpression
                                        case 0b101 | 0b0000000: { // SRL
                                            invokeOp("srl", rd, rs1, rs2);

                                            break;
                                        }
                                        case 0b101 | 0b0100000: { // SRA
                                            invokeOp("sra", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b110: { // OR
                                            invokeOp("or", rd, rs1, rs2);
                                            break;
                                        }
                                        case 0b111: { // AND
                                            invokeOp("and", rd, rs1, rs2);
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

                            instOffset += 4;
                            break;
                        }

                        case 0b1101111: { // JAL
                            invokeOp("jal", inst, rd, instOffset + request.toPC);
                            return; // May have jumped out of page. Also avoid infinite loops.
                        }

                        case 0b110_0111: { // JALR
                            invokeOp("jalr", inst, rd, rs1, instOffset + request.toPC);
                            return; // May have jumped out of page. Also avoid infinite loops.
                        }

                        case 0b1100011: { // BRANCH
                            invokeOp(boolean.class, "branch", inst, rs1, instOffset + request.toPC);
                            mv.visitJumpInsn(Opcodes.IFNE, returnLabel);

                            instOffset += 4;
                            break;
                        }

                        case 0b0000011: { // LOAD
                            invokeOp("load", inst, rd, rs1);

                            instOffset += 4;
                            break;
                        }

                        case 0b0100011: { // STORE
                            invokeOp("store", inst, rs1);

                            instOffset += 4;
                            break;
                        }

                        case 0b0001111: { // MISC-MEM
                            final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                            switch (funct3) {
                                case 0b000: { // FENCE
//                                    if ((inst & 0b1111_00000000_11111_111_11111_0000000) != 0) // Not supporting any flags.
//                                        throw new R5IllegalInstructionException(inst);
                                    break;
                                }

                                case 0b001: { // FENCE.I
                                    if (inst != 0b000000000000_00000_001_00000_0001111)
                                        throw new R5IllegalInstructionException(inst);
                                    break;
                                }

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }

                            instOffset += 4;
                            break;
                        }

                        case 0b1110011: { // SYSTEM
                            final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                            if (funct3 == 0b100) {
                                throw new R5IllegalInstructionException(inst);
                            }

                            switch (funct3 & 0b11) {
                                case 0b00: { // PRIV
                                    final int funct12 = inst >>> 20;
                                    switch (funct12) {
                                        case 0b0000000_00000: { // ECALL
                                            if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                                throw new R5IllegalInstructionException(inst);
                                            }

                                            storePCInLocal();
                                            mv.visitTypeInsn(Opcodes.NEW, ECALL_EXCEPTION_TYPE.getInternalName());
                                            mv.visitInsn(Opcodes.DUP);
                                            mv.visitVarInsn(Opcodes.ALOAD, CPU_LOCAL_INDEX);
                                            mv.visitFieldInsn(Opcodes.GETFIELD, CPU_TYPE.getInternalName(), "priv", Type.INT_TYPE.getDescriptor());
                                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ECALL_EXCEPTION_TYPE.getInternalName(), INIT_INT.getName(), INIT_INT.getDescriptor(), false);
                                            mv.visitInsn(Opcodes.ATHROW);
                                            return;
                                        }
                                        case 0b0000000_00001: { // EBREAK
                                            if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                                throw new R5IllegalInstructionException(inst);
                                            }

                                            storePCInLocal();
                                            mv.visitTypeInsn(Opcodes.NEW, BREAKPOINT_EXCEPTION_TYPE.getInternalName());
                                            mv.visitInsn(Opcodes.DUP);
                                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, BREAKPOINT_EXCEPTION_TYPE.getInternalName(), INIT_VOID.getName(), INIT_VOID.getDescriptor(), false);
                                            mv.visitInsn(Opcodes.ATHROW);
                                            return;
                                        }
                                        case 0b0001000_00010: { // SRET
                                            if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                                throw new R5IllegalInstructionException(inst);
                                            }

                                            invokeOp("sret", inst);
                                            return; // Invalidate fetch cache
                                        }
                                        case 0b0011000_00010: { // MRET
                                            if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                                throw new R5IllegalInstructionException(inst);
                                            }

                                            invokeOp("mret", inst);
                                            return; // Invalidate fetch cache
                                        }

                                        case 0b0001000_00101: { // WFI
                                            invokeOp(boolean.class, "wfi", inst);

                                            final Label noreturn = new Label();
                                            mv.visitJumpInsn(Opcodes.IFEQ, noreturn);
                                            storePCInCPU(4);
                                            mv.visitJumpInsn(Opcodes.GOTO, returnLabel);
                                            mv.visitLabel(noreturn);

                                            break;
                                        }

                                        default: {
                                            final int funct7 = funct12 >>> 5;
                                            if (funct7 == 0b0001001) { // SFENCE.VMA
                                                invokeOp("sfence_vma", inst, rs1);
                                                storePCInCPU(4);
                                                mv.visitJumpInsn(Opcodes.GOTO, returnLabel);
                                                return; // Invalidate fetch cache
                                            } else {
                                                throw new R5IllegalInstructionException(inst);
                                            }
                                        }
                                    }
                                }

                                case 0b01:
                                case 0b10:
                                case 0b11: {
                                    final int csr = inst >>> 20;
                                    final int funct3lb = funct3 & 0b11;
                                    switch (funct3lb) {
                                        case 0b01: { // CSRRW[I]
                                            invokeOp(boolean.class, "csrrw", inst, rd, rs1, funct3, csr);

                                            final Label noreturn = new Label();
                                            mv.visitJumpInsn(Opcodes.IFEQ, noreturn);
                                            storePCInCPU(4);
                                            mv.visitJumpInsn(Opcodes.GOTO, returnLabel);
                                            mv.visitLabel(noreturn);

                                            break;
                                        }
                                        case 0b10:   // CSRRS[I]
                                        case 0b11: { // CSRRC[I]
                                            invokeOp(boolean.class, "csrrx", inst, rd, rs1, funct3, csr);

                                            final Label noreturn = new Label();
                                            mv.visitJumpInsn(Opcodes.IFEQ, noreturn);
                                            storePCInCPU(4);
                                            mv.visitJumpInsn(Opcodes.GOTO, returnLabel);
                                            mv.visitLabel(noreturn);

                                            break;
                                        }
                                    }
                                    break;
                                }
                            }

                            instOffset += 4;
                            break;
                        }

                        case 0b0101111: { // AMO
                            final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                            if (funct3 == 0b010) { // 32 bit
                                final int rs2 = BitUtils.getField(inst, 20, 24, 0);
                                final int funct5 = inst >>> 27;
                                switch (funct5) {
                                    case 0b00010: { // LR.W
                                        if (rs2 != 0) {
                                            throw new R5IllegalInstructionException(inst);
                                        }

                                        invokeOp("lr_w", rd, rs1);
                                        break;
                                    }
                                    case 0b00011: { // SC.W
                                        invokeOp("sc_w", rd, rs1, rs2);
                                        break;
                                    }

                                    case 0b00001: { // AMOSWAP.W
                                        invokeOp("amoswap_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b00000: { // AMOADD.W
                                        invokeOp("amoadd_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b00100: { // AMOXOR.W
                                        invokeOp("amoxor_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b01100: { // AMOAND.W
                                        invokeOp("amoand_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b01000: { // AMOOR.W
                                        invokeOp("amoor_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b10000: { // AMOMIN.W
                                        invokeOp("amomin_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b10100: { // AMOMAX.W
                                        invokeOp("amomax_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b11000: { // AMOMINU.W
                                        invokeOp("amominu_w", rd, rs1, rs2);
                                        break;
                                    }
                                    case 0b11100: { // AMOMAXU.W
                                        invokeOp("amomaxu_w", rd, rs1, rs2);
                                        break;
                                    }

                                    default: {
                                        throw new R5IllegalInstructionException(inst);
                                    }
                                }
                            } else {
                                throw new R5IllegalInstructionException(inst);
                            }

                            instOffset += 4;
                            break;
                        }

                        default: {
                            throw new R5IllegalInstructionException(inst);
                        }
                    }
                } else {
                    if (inst == 0) {
                        throw new R5IllegalInstructionException(inst);
                    }

                    final int op = inst & 0b11;
                    final int funct3 = BitUtils.getField(inst, 13, 15, 0);

                    switch (op) {
                        case 0b00: { // Quadrant 0
                            final int rd = BitUtils.getField(inst, 2, 4, 0) + 8;
                            switch (funct3) {
                                case 0b000: { // C.ADDI4SPN
                                    invokeOp("c_addi4spn", inst, rd);
                                    break;
                                }
                                // 0b001: C.FLD
                                case 0b010: { // C.LW
                                    invokeOp("c_lw", inst, rd);
                                    break;
                                }
                                // 0b011: C.FLW
                                // 0b101: C.FSD
                                case 0b110: { // C.SW
                                    invokeOp("c_sw", inst, rd);
                                    break;
                                }
                                // 0b111: C.FSW

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }

                            instOffset += 2;
                            break;
                        }

                        case 0b01: { // Quadrant 1
                            switch (funct3) {
                                case 0b000: {
                                    final int rd = BitUtils.getField(inst, 7, 11, 0);
                                    if (rd != 0) { // C.ADDI
                                        invokeOp("c_addi", inst);
                                    } // else: C.NOP

                                    instOffset += 2;
                                    break;
                                }
                                case 0b001: { // C.JAL
                                    invokeOp("c_jal", inst, instOffset + request.toPC);
                                    return; // May have jumped out of page. Also avoid infinite loops.
                                }
                                case 0b010: { // C.LI
                                    invokeOp("c_li", inst);

                                    instOffset += 2;
                                    break;
                                }
                                case 0b011: {
                                    final int rd = BitUtils.getField(inst, 7, 11, 0);
                                    if (rd == 2) { // C.ADDI16SP
                                        invokeOp("c_addi16sp", inst);
                                    } else if (rd != 0) { // C.LUI
                                        invokeOp("c_lui", inst, rd);
                                    } // else: HINT

                                    instOffset += 2;
                                    break;
                                }
                                case 0b100: {
                                    final int funct2 = BitUtils.getField(inst, 10, 11, 0);
                                    final int rd = BitUtils.getField(inst, 7, 9, 0) + 8;
                                    switch (funct2) {
                                        case 0b00:   // C.SRLI
                                        case 0b01: { // C.SRAI
                                            invokeOp("c_srxi", inst, funct2, rd);
                                            break;
                                        }
                                        case 0b10: { // C.ANDI
                                            invokeOp("c_andi", inst, rd);
                                            break;
                                        }
                                        case 0b11: {
                                            final int funct3b = BitUtils.getField(inst, 5, 6, 0) |
                                                                BitUtils.getField(inst, 12, 12, 2);
                                            final int rs2 = BitUtils.getField(inst, 2, 4, 0) + 8;
                                            switch (funct3b) {
                                                case 0b000: { // C.SUB
                                                    invokeOp("c_sub", rd, rs2);
                                                    break;
                                                }
                                                case 0b001: { // C.XOR
                                                    invokeOp("c_xor", rd, rs2);
                                                    break;
                                                }
                                                case 0b010: { // C.OR
                                                    invokeOp("c_or", rd, rs2);
                                                    break;
                                                }
                                                case 0b011: { // C.AND
                                                    invokeOp("c_and", rd, rs2);
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

                                    instOffset += 2;
                                    break;
                                }
                                case 0b101: { // C.J
                                    invokeOp("c_j", inst, instOffset + request.toPC);
                                    return; // May have jumped out of page. Also avoid infinite loops.
                                }
                                case 0b110:   // C.BEQZ
                                case 0b111: { // C.BNEZ
                                    invokeOp(boolean.class, "c_branch", inst, funct3, instOffset + request.toPC);
                                    mv.visitJumpInsn(Opcodes.IFNE, returnLabel);

                                    instOffset += 2;
                                    break;
                                }

                                default: {
                                    throw new R5IllegalInstructionException(inst);
                                }
                            }

                            break;
                        }

                        case 0b10: { // Quadrant 2
                            final int rd = BitUtils.getField(inst, 7, 11, 0);
                            switch (funct3) {
                                case 0b000: { // C.SLLI
                                    invokeOp("c_slli", inst, rd);

                                    instOffset += 2;
                                    break;
                                }
                                // 0b001: C.FLDSP
                                case 0b010: { // C.LWSP
                                    if (rd == 0) { // Reserved.
                                        throw new R5IllegalInstructionException(inst);
                                    }

                                    invokeOp("c_lwsp", inst, rd);

                                    instOffset += 2;
                                    break;
                                }
                                // 0b011: C.FLWSP
                                case 0b100: {
                                    final int rs2 = BitUtils.getField(inst, 2, 6, 0);
                                    if ((inst & (1 << 12)) == 0) {
                                        if (rs2 == 0) { // C.JR
                                            if (rd == 0) {
                                                throw new R5IllegalInstructionException(inst);
                                            }

                                            invokeOp("c_jr", rd);
                                            return; // May have jumped out of page. Also avoid infinite loops.
                                        } else { // C.MV
                                            invokeOp("c_mv", rd, rs2);

                                            instOffset += 2;
                                        }
                                    } else {
                                        if (rs2 == 0) {
                                            if (rd == 0) { // C.EBREAK
                                                storePCInLocal();
                                                mv.visitTypeInsn(Opcodes.NEW, BREAKPOINT_EXCEPTION_TYPE.getInternalName());
                                                mv.visitInsn(Opcodes.DUP);
                                                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, BREAKPOINT_EXCEPTION_TYPE.getInternalName(), INIT_VOID.getName(), INIT_VOID.getDescriptor(), false);
                                                mv.visitInsn(Opcodes.ATHROW);
                                                return;
                                            } else { // C.JALR
                                                invokeOp("c_jalr", rd, instOffset + request.toPC);
                                                return; // May have jumped out of page. Also avoid infinite loops.
                                            }
                                        } else { // C.ADD
                                            invokeOp("c_add", rd, rs2);

                                            instOffset += 2;
                                        }
                                    }

                                    break;
                                }
                                // 0b101: C.FSDSP
                                case 0b110: { // C.SWSP
                                    invokeOp("c_swsp", inst);

                                    instOffset += 2;
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
                }

                if (instOffset < instEnd) { // Likely case: we're still fully in the page.
                    inst = request.device.load(instOffset, Sizes.SIZE_32_LOG2);
                } else { // Unlikely case: we reached the end of the page. Leave to do interrupts and cycle check.
                    storePCInCPU();
                    return;
                }
            }
        } catch (final R5IllegalInstructionException e) {
            storePCInCPU();
            mv.visitTypeInsn(Opcodes.NEW, ILLEGAL_INSTRUCTION_EXCEPTION_TYPE.getInternalName());
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(e.getExceptionValue());
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ILLEGAL_INSTRUCTION_EXCEPTION_TYPE.getInternalName(), INIT_INT.getName(), INIT_INT.getDescriptor(), false);
            mv.visitInsn(Opcodes.ATHROW);
        } catch (final MemoryAccessException e) {
            final Type type = Type.getType(e.getClass());
            storePCInCPU();
            mv.visitTypeInsn(Opcodes.NEW, type.getInternalName());
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(e.getAddress());
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), INIT_INT.getName(), INIT_INT.getDescriptor(), false);
            mv.visitInsn(Opcodes.ATHROW);
        } finally {
            mv.visitLabel(returnLabel);
            mv.visitInsn(Opcodes.RETURN);

            mv.visitLabel(catchLabel);

            mv.visitVarInsn(Opcodes.ALOAD, CPU_LOCAL_INDEX);
            mv.visitVarInsn(Opcodes.ILOAD, PC_LOCAL_INDEX);
            mv.visitFieldInsn(Opcodes.PUTFIELD, CPU_TYPE.getInternalName(), "pc", Type.INT_TYPE.getDescriptor());
            mv.visitInsn(Opcodes.ATHROW);

            mv.visitLabel(endLabel);
        }
    }

    private void generateCPULocal() {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, className, CPU_FIELD_NAME, CPU_TYPE.getDescriptor());
        mv.visitVarInsn(Opcodes.ASTORE, CPU_LOCAL_INDEX);
    }

    private void generatePCLocal() {
        mv.visitLdcInsn(instOffset);
        mv.visitVarInsn(Opcodes.ISTORE, PC_LOCAL_INDEX);
    }

    private void generateMcycleLocal() {
        mv.visitVarInsn(Opcodes.ALOAD, CPU_LOCAL_INDEX);
        mv.visitFieldInsn(Opcodes.GETFIELD, CPU_TYPE.getInternalName(), "mcycle", Type.LONG_TYPE.getDescriptor());
        mv.visitVarInsn(Opcodes.LSTORE, MCYCLE_LOCAL_INDEX);
    }

    private void incCycle() {
        mv.visitVarInsn(Opcodes.ALOAD, CPU_LOCAL_INDEX);
        mv.visitVarInsn(Opcodes.LLOAD, MCYCLE_LOCAL_INDEX);
        mv.visitLdcInsn(1L);
        mv.visitInsn(Opcodes.LADD);
        mv.visitInsn(Opcodes.DUP2);
        mv.visitVarInsn(Opcodes.LSTORE, MCYCLE_LOCAL_INDEX);
        mv.visitFieldInsn(Opcodes.PUTFIELD, CPU_TYPE.getInternalName(), "mcycle", Type.LONG_TYPE.getDescriptor());
    }

    private void invokeOp(final String methodName, final int... args) {
        invokeOp(null, methodName, args);
    }

    private void invokeOp(@Nullable final Class<?> expectedReturnType, final String methodName, final int... args) {
        final OpcodeMethod method = getOpcodeMethod(methodName);
        if (method.argTypes.length != args.length) {
            throw new IllegalArgumentException("invalid argument count for method [" + methodName + "]");
        }

        if (expectedReturnType != null && method.returnType != expectedReturnType) {
            throw new IllegalArgumentException("invalid return type for method [" + methodName + "]");
        }

        // Update pc local if method may throw an exception so we can path the pc field in the cpu
        // appropriately in the exception handler in case it does throw an exception.
        // Note that RuntimeExceptions will bypass this and completely break the program state.
        // However, we can expect that these will bubble through to the top anyway, so we don't care.
        if (method.throwsExceptions) {
            storePCInLocal();
        }

        mv.visitVarInsn(Opcodes.ALOAD, CPU_LOCAL_INDEX);

        for (int i = 0; i < args.length; i++) {
            if (!ClassUtils.isAssignable(method.argTypes[i], int.class)) {
                throw new IllegalArgumentException("invalid argument type for argument [" + (i + 1) + "] for method [" + methodName + "]");
            }
            mv.visitLdcInsn(args[i]);
        }

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CPU_TYPE.getInternalName(), methodName, method.descriptor, false);

        if (expectedReturnType == null && method.returnType != void.class) {
            mv.visitInsn(Opcodes.POP);
        }
    }

    private void storePCInLocal() {
        mv.visitLdcInsn(instOffset + request.toPC);
        mv.visitVarInsn(Opcodes.ISTORE, PC_LOCAL_INDEX);
    }

    private void storePCInCPU() {
        storePCInCPU(0);
    }

    private void storePCInCPU(final int offset) {
        mv.visitVarInsn(Opcodes.ALOAD, CPU_LOCAL_INDEX);
        mv.visitLdcInsn(instOffset + request.toPC + offset);
        mv.visitFieldInsn(Opcodes.PUTFIELD, CPU_TYPE.getInternalName(), "pc", Type.INT_TYPE.getInternalName());
    }

    private static OpcodeMethod getOpcodeMethod(final String name) {
        return OPCODE_METHODS.computeIfAbsent(name, Translator::findOpcodeMethod);
    }

    private static OpcodeMethod findOpcodeMethod(final String name) {
        for (final java.lang.reflect.Method method : R5CPU.class.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                method.setAccessible(true);
                return new OpcodeMethod(method);
            }
        }

        throw new IllegalArgumentException("invalid method name [" + name + "]");
    }

    private static final class OpcodeMethod {
        final Class<?>[] argTypes;
        final Class<?> returnType;
        final boolean throwsExceptions;
        final String descriptor;

        OpcodeMethod(final java.lang.reflect.Method method) {
            argTypes = method.getParameterTypes();
            returnType = method.getReturnType();
            throwsExceptions = method.getExceptionTypes().length > 0;
            descriptor = Type.getType(method).getDescriptor();
        }
    }
}
