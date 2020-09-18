package li.cil.circuity.vm.riscv;

import li.cil.circuity.vm.BitUtils;

public final class R5Disassembler {
    private static final String ILLEGAL_INSTRUCTION = "<???>";
    private static final String HINT = "<hint>";
    private static final String OP_SEP = "\t\t; ";

    public static String disassemble(final int instruction) {
        if ((instruction & 0b11) == 0b11) {
            return disassembleUncompressed(instruction);
        } else {
            return disassembleCompressed(instruction);
        }
    }

    private static String disassembleUncompressed(final int inst) {
        final int opcode = BitUtils.getField(inst, 0, 6, 0);
        final int rd = BitUtils.getField(inst, 7, 11, 0);
        final int rs1 = BitUtils.getField(inst, 15, 19, 0);
        final int rs2 = BitUtils.getField(inst, 20, 24, 0);

        switch (opcode) {
            case 0b0010011: {
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                final int imm = inst >> 20; // inst[31:20], sign extended
                switch (funct3) {
                    case 0b000: {
                        return op("addi", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = %s %s %d", reg(rd), reg(rs1), "+", imm);
                    }
                    case 0b010: {
                        return op("slti", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = %s %s %d ? 1 : 0", reg(rd), reg(rs1), "<", imm);
                    }
                    case 0b011: {
                        return op("sltiu", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = (uint)%s %s %d ? 1 : 0", reg(rd), reg(rs1), "<", imm);
                    }
                    case 0b100: {
                        return op("xori", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = %s %s %d", reg(rd), reg(rs1), "^", imm);
                    }
                    case 0b110: {
                        return op("ori", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = %s %s %d", reg(rd), reg(rs1), "|", imm);
                    }
                    case 0b111: {
                        return op("andi", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = %s %s %d", reg(rd), reg(rs1), "&", imm);
                    }
                    case 0b001: {
                        if ((inst & 0b1111111_00000_00000_000_00000_0000000) != 0)
                            return ILLEGAL_INSTRUCTION;

                        return op("slli", reg(rd), reg(rs1), imm) + OP_SEP +
                               String.format("%s = %s %s %d", reg(rd), reg(rs1), "<<", imm);
                    }
                    case 0b101: {
                        final int funct7 = BitUtils.getField(imm, 5, 11, 0); // imm[11:5]
                        switch (funct7) {
                            case 0b0000000: {
                                return op("srli", reg(rd), reg(rs1), imm) + OP_SEP +
                                       String.format("%s = %s %s %d", reg(rd), reg(rs1), ">>>", imm);
                            }
                            case 0b0100000: {
                                return op("srai", reg(rd), reg(rs1), imm) + OP_SEP +
                                       String.format("%s = %s %s %d", reg(rd), reg(rs1), ">>", imm);
                            }
                        }
                    }
                }

                break;
            }

            case 0b0110111: {
                final int imm = inst & 0b11111111111111111111_00000_0000000; // inst[31:12]
                return op("lui", reg(rd), imm) + OP_SEP +
                       String.format("%s = %d", reg(rd), imm);
            }

            case 0b0010111: {
                final int imm = inst & 0b11111111111111111111_00000_0000000; // inst[31:12]
                return op("auipc", reg(rd), imm) + OP_SEP +
                       String.format("%s = pc + %d", reg(rd), imm);
            }

            case 0b0110011: {
                final int funct7 = BitUtils.getField(inst, 25, 31, 0);
                switch (funct7) {
                    case 0b000001: {
                        final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                        switch (funct3) {
                            case 0b000: {
                                return op("mul", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "*", reg(rs2));
                            }
                            case 0b001: {
                                return op("mulh", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "*", reg(rs2));
                            }
                            case 0b010: {
                                return op("mulhsu", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "*", reg(rs2));
                            }
                            case 0b011: {
                                return op("mulhu", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "*", reg(rs2));
                            }

                            case 0b100: {
                                return op("div", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "/", reg(rs2));
                            }
                            case 0b101: {
                                return op("divu", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "/", reg(rs2));
                            }
                            case 0b110: { // REM
                                return op("rem", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "%", reg(rs2));
                            }
                            case 0b111: { // REMU
                                return op("remu", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "%", reg(rs2));
                            }
                        }

                        break;
                    }
                    case 0b0000000:
                    case 0b0100000: {
                        final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                        switch (funct3 | funct7) {
                            case 0b000: {
                                return op("add", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "+", reg(rs2));
                            }
                            case 0b000 | 0b0100000: {
                                return op("sub", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "-", reg(rs2));
                            }
                            case 0b001: {
                                return op("sll", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "<<", reg(rs2));
                            }
                            case 0b010: {
                                return op("slt", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s ? 1 : 0", reg(rd), reg(rs1), "<", reg(rs2));
                            }
                            case 0b011: {
                                return op("sltu", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = (uint)%s %s (uint)%s ? 1 : 0", reg(rd), reg(rs1), "<", reg(rs2));
                            }
                            case 0b100: {
                                return op("xor", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "^", reg(rs2));
                            }
                            case 0b101: {
                                return op("srl", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), ">>>", reg(rs2));
                            }
                            case 0b101 | 0b0100000: {
                                return op("sra", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), ">>", reg(rs2));
                            }
                            case 0b110: {
                                return op("or", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "|", reg(rs2));
                            }
                            case 0b111: {
                                return op("and", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = %s %s %s", reg(rd), reg(rs1), "&", reg(rs2));
                            }
                        }

                        break;
                    }
                }

                break;
            }

            case 0b1101111: {

                final int imm = BitUtils.extendSign(BitUtils.getField(inst, 31, 31, 20) |
                                                    BitUtils.getField(inst, 21, 30, 1) |
                                                    BitUtils.getField(inst, 20, 20, 11) |
                                                    BitUtils.getField(inst, 12, 19, 12), 20);
                return op("jal", reg(rd), imm) + OP_SEP +
                       String.format("pc += %d", imm);
            }

            case 0b110_0111: {
                final int imm = inst >> 20; // inst[31:20], sign extended
                return op("jalr", reg(rd), reg(rs1), imm) + OP_SEP +
                       ((rd != 0)
                               ? String.format("%s = pc + 4, ", reg(rd))
                               : "") +
                       String.format("pc = %s + %d", reg(rs1), imm);
            }

            case 0b1100011: {

                final int imm = BitUtils.extendSign(BitUtils.getField(inst, 31, 31, 12) |
                                                    BitUtils.getField(inst, 25, 30, 5) |
                                                    BitUtils.getField(inst, 8, 11, 1) |
                                                    BitUtils.getField(inst, 7, 7, 11), 13);
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                switch (funct3) {
                    case 0b000: {
                        return op("beq", reg(rs1), reg(rs2), imm) + OP_SEP +
                               String.format("if (%s == %s) pc += %d", reg(rs1), reg(rs2), imm);
                    }
                    case 0b001: {
                        return op("bne", reg(rs1), reg(rs2), imm) + OP_SEP +
                               String.format("if (%s != %s) pc += %d", reg(rs1), reg(rs2), imm);
                    }
                    case 0b100: {
                        return op("blt", reg(rs1), reg(rs2), imm) + OP_SEP +
                               String.format("if (%s < %s) pc += %d", reg(rs1), reg(rs2), imm);
                    }
                    case 0b101: {
                        return op("bge", reg(rs1), reg(rs2), imm) + OP_SEP +
                               String.format("if (%s >= %s) pc += %d", reg(rs1), reg(rs2), imm);
                    }
                    case 0b110: {
                        return op("bltu", reg(rs1), reg(rs2), imm) + OP_SEP +
                               String.format("if ((uint)%s < (uint)%s) pc += %d", reg(rs1), reg(rs2), imm);
                    }
                    case 0b111: {
                        return op("bgeu", reg(rs1), reg(rs2), imm) + OP_SEP +
                               String.format("if ((uint)%s >= (uint)%s) pc += %d", reg(rs1), reg(rs2), imm);
                    }
                }

                break;
            }

            case 0b0000011: {
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                final int imm = inst >> 20; // inst[31:20], sign extended
                switch (funct3) {
                    case 0b000: {
                        return op("lb", reg(rd), addr(rs1, imm)) + OP_SEP +
                               String.format("%s = *(int8*)(%s + %d)", reg(rd), reg(rs1), imm);
                    }
                    case 0b001: {
                        return op("lh", reg(rd), addr(rs1, imm)) + OP_SEP +
                               String.format("%s = *(int16*)(%s + %d)", reg(rd), reg(rs1), imm);
                    }
                    case 0b010: {
                        return op("lw", reg(rd), addr(rs1, imm)) + OP_SEP +
                               String.format("%s = *(int32*)(%s + %d)", reg(rd), reg(rs1), imm);
                    }
                    case 0b100: {
                        return op("lbu", reg(rd), addr(rs1, imm)) + OP_SEP +
                               String.format("%s = *(uint8*)(%s + %d)", reg(rd), reg(rs1), imm);
                    }
                    case 0b101: {
                        return op("lhu", reg(rd), addr(rs1, imm)) + OP_SEP +
                               String.format("%s = *(uint16*)(%s + %d)", reg(rd), reg(rs1), imm);
                    }
                }

                break;
            }

            case 0b0100011: {
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                final int imm = BitUtils.getField(inst, 25, 31, 5) |
                                BitUtils.getField(inst, 7, 11, 0);
                switch (funct3) {
                    case 0b000: {
                        return op("sb", reg(rs2), addr(rs1, imm)) + OP_SEP +
                               String.format("*(int8*)(%s + %d) = %s", reg(rs1), imm, reg(rs2));
                    }
                    case 0b001: {
                        return op("sh", reg(rs2), addr(rs1, imm)) + OP_SEP +
                               String.format("*(int16*)(%s + %d) = %s", reg(rs1), imm, reg(rs2));
                    }
                    case 0b010: {
                        return op("sw", reg(rs2), addr(rs1, imm)) + OP_SEP +
                               String.format("*(int32*)(%s + %d) = %s", reg(rs1), imm, reg(rs2));
                    }
                }

                break;
            }

            case 0b0001111: {
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                switch (funct3) {
                    case 0b000: {
                        return op("fence");
                    }

                    case 0b001: {
                        if (inst != 0b000000000000_00000_001_00000_0001111)
                            return ILLEGAL_INSTRUCTION;
                        return op("fence.i");
                    }
                }

                break;
            }

            case 0b1110011: {
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                if (funct3 == 0b100) {
                    return ILLEGAL_INSTRUCTION;
                }

                switch (funct3 & 0b11) {
                    case 0b00: {
                        final int funct12 = inst >>> 20; // inst[31:20], not sign-extended
                        switch (funct12) {
                            case 0b0000000_00000: {
                                if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("ecall");
                            }
                            case 0b0000000_00001: {
                                if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("ebreak");
                            }
                            // 0b0000000_00010: URET
                            case 0b0001000_00010: {
                                if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("sret");
                            }
                            case 0b0011000_00010: {
                                if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("mret");
                            }

                            case 0b0001000_00101: { // WFI
                                if ((inst & 0b000000000000_11111_111_11111_0000000) != 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("wfi");
                            }

                            default: {
                                final int funct7 = funct12 >>> 5;
                                if (funct7 == 0b0001001) {
                                    if ((inst & 0b0000000_00000_00000_111_11111_0000000) != 0) {
                                        return ILLEGAL_INSTRUCTION;
                                    }

                                    return op("sfence.vma");
                                }

                                break;
                            }
                        }
                    }

                    case 0b01:
                    case 0b10:
                    case 0b11: {
                        final int csr = inst >>> 20; // inst[31:20], not sign-extended
                        switch (funct3) {
                            case 0b001: {
                                return op("csrrw", reg(rd), csr2n(csr), reg(rs1)) + OP_SEP +
                                       ((rd != 0)
                                               ? String.format("%s = %s, %s = %s", reg(rd), csr2n(csr), csr2n(csr), reg(rs1))
                                               : String.format("%s = %s", csr2n(csr), reg(rs1)));
                            }
                            case 0b010: {
                                return op("csrrs", reg(rd), csr2n(csr), reg(rs1)) + OP_SEP +
                                       ((rd != 0)
                                               ? String.format("%s = %s", reg(rd), csr2n(csr))
                                               : String.format("*%s", csr2n(csr))) +
                                       ((rs1 != 0)
                                               ? String.format(", %s |= %s", csr2n(csr), reg(rs1))
                                               : "");
                            }
                            case 0b011: {
                                return op("csrrc", reg(rd), csr2n(csr), reg(rs1)) + OP_SEP +
                                       ((rd != 0)
                                               ? String.format("%s = %s", reg(rd), csr2n(csr))
                                               : String.format("*%s", csr2n(csr))) +
                                       ((rs1 != 0)
                                               ? String.format(", %s &= ~%s", csr2n(csr), reg(rs1))
                                               : "");
                            }
                            case 0b101: {
                                return op("csrrwi", reg(rd), csr2n(csr), rs1) + OP_SEP +
                                       ((rd != 0)
                                               ? String.format("%s = %s, %s = %d", reg(rd), csr2n(csr), csr2n(csr), rs1)
                                               : String.format("%s = %d", csr2n(csr), rs1));
                            }
                            case 0b110: {
                                return op("csrrsi", rd, csr2n(csr), rs1) + OP_SEP +
                                       ((rd != 0)
                                               ? String.format("%s = %s", reg(rd), csr2n(csr))
                                               : String.format("*%s", csr2n(csr))) +
                                       ((rs1 != 0)
                                               ? String.format(", %s |= %s", csr2n(csr), rs1)
                                               : "");
                            }
                            case 0b111: {
                                return op("csrrci", rd, csr2n(csr), rs1) + OP_SEP +
                                       ((rd != 0)
                                               ? String.format("%s = %s", reg(rd), csr2n(csr))
                                               : String.format("*%s", csr2n(csr))) +
                                       ((rs1 != 0)
                                               ? String.format(", %s &= ~%s", csr2n(csr), rs1)
                                               : "");
                            }
                        }
                        break;
                    }
                }

                break;
            }

            case 0b0101111: {
                final int funct3 = BitUtils.getField(inst, 12, 14, 0);
                switch (funct3) { // width
                    case 0b010: { // 32
                        final int funct5 = inst >>> 27; // inst[31:27], not sign-extended
                        switch (funct5) {
                            case 0b00010: {
                                if (rs2 != 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("lr.w", reg(rd), reg(rs1)) + OP_SEP +
                                       String.format("%s = *%s, reserve %s", reg(rd), reg(rs1), reg(rs1));
                            }
                            case 0b00011: {
                                return op("sc.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("if (%s reserved) *%s = %s, %s = 0 else %s = 1", reg(rs1), reg(rs1), reg(rs2), reg(rd), reg(rd));
                            }

                            case 0b00001: {
                                return op("amoswap.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = *%s %s", reg(rd), reg(rs1), reg(rs1), reg(rs1), reg(rs2));
                            }
                            case 0b00000: {
                                return op("amoadd.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = *%s %s %s", reg(rd), reg(rs1), reg(rs1), reg(rs1), "+", reg(rs2));
                            }
                            case 0b00100: {
                                return op("amoxor.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = *%s %s %s", reg(rd), reg(rs1), reg(rs1), reg(rs1), "^", reg(rs2));
                            }
                            case 0b01100: {
                                return op("amoand.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = *%s %s %s", reg(rd), reg(rs1), reg(rs1), reg(rs1), "&", reg(rs2));
                            }
                            case 0b01000: {
                                return op("amoor.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = *%s %s %s", reg(rd), reg(rs1), reg(rs1), reg(rs1), "|", reg(rs2));
                            }
                            case 0b10000: {
                                return op("amomin.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = min(*%s, %s)", reg(rd), reg(rs1), reg(rs1), reg(rs1), reg(rs2));
                            }
                            case 0b10100: {
                                return op("amomax.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = max(*%s, %s)", reg(rd), reg(rs1), reg(rs1), reg(rs1), reg(rs2));
                            }
                            case 0b11000: {
                                return op("amominu.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = min(*(uint32*)%s, (uint32)%s)", reg(rd), reg(rs1), reg(rs1), reg(rs1), reg(rs2));
                            }
                            case 0b11100: {
                                return op("amomaxu.w", reg(rd), reg(rs1), reg(rs2)) + OP_SEP +
                                       String.format("%s = *%s, *%s = max(*(uint32*)%s, (uint32)%s)", reg(rd), reg(rs1), reg(rs1), reg(rs1), reg(rs2));
                            }
                        }

                        break;
                    }
                    case 0b011: { // 64
                        return ILLEGAL_INSTRUCTION;
                    }
                }

                break;
            }
        }

        return ILLEGAL_INSTRUCTION;
    }

    private static String disassembleCompressed(final int inst) {
        if (inst == 0) {
            return ILLEGAL_INSTRUCTION;
        }

        final int op = inst & 0b11;
        switch (op) {
            case 0b00: { // Quadrant 0
                final int funct3 = BitUtils.getField(inst, 13, 15, 0);
                final int rd = BitUtils.getField(inst, 2, 4, 0) + 8;
                switch (funct3) {
                    case 0b000: {
                        final int imm = BitUtils.getField(inst, 11, 12, 4) |
                                        BitUtils.getField(inst, 7, 10, 6) |
                                        BitUtils.getField(inst, 6, 6, 2) |
                                        BitUtils.getField(inst, 5, 5, 3);
                        if (imm == 0) {
                            return ILLEGAL_INSTRUCTION;
                        }
                        return op("c.addi4spn", reg(rd), imm) + OP_SEP +
                               String.format("%s = %s + %d", reg(rd), reg(2), imm);
                    }
                    // 0b001: C.FLD
                    case 0b010: {
                        final int offset = BitUtils.getField(inst, 10, 12, 3) |
                                           BitUtils.getField(inst, 6, 6, 2) |
                                           BitUtils.getField(inst, 5, 5, 6);
                        final int rs1 = BitUtils.getField(inst, 7, 9, 0) + 8; // V1p100
                        return op("c.lw", reg(rd), addr(rs1, offset)) + OP_SEP +
                               String.format("%s = *(uint32*)(%s + %d)", reg(rd), reg(rs1), offset);
                    }
                    // 0b011: C.FLW
                    // 0b101: C.FSD
                    case 0b110: {
                        final int offset = BitUtils.getField(inst, 10, 12, 3) |
                                           BitUtils.getField(inst, 6, 6, 2) |
                                           BitUtils.getField(inst, 5, 5, 6);
                        final int rs1 = BitUtils.getField(inst, 7, 9, 0) + 8; // V1p100
                        return op("c.sw", reg(rd), addr(rs1, offset)) + OP_SEP +
                               String.format("*(uint32*)(%s + %d) = %s", reg(rs1), offset, reg(rd));
                    }
                    // 0b111: C.FSW
                }

                break;
            }

            case 0b01: { // Quadrant 1
                final int funct3 = BitUtils.getField(inst, 13, 15, 0);
                switch (funct3) {
                    case 0b000: {

                        final int imm = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 5) |
                                                            BitUtils.getField(inst, 2, 6, 0), 6);
                        final int rd = BitUtils.getField(inst, 7, 11, 0);
                        if (rd != 0) {
                            return op("c.addi", reg(rd), imm) + OP_SEP +
                                   String.format("%s += %d", reg(rd), imm);
                        } else if (imm != 0) {
                            return op("nop");
                        } else {
                            return HINT;
                        }
                    }
                    case 0b001: {

                        final int offset = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 11) |
                                                               BitUtils.getField(inst, 11, 11, 4) |
                                                               BitUtils.getField(inst, 9, 10, 8) |
                                                               BitUtils.getField(inst, 8, 8, 10) |
                                                               BitUtils.getField(inst, 7, 7, 6) |
                                                               BitUtils.getField(inst, 6, 6, 7) |
                                                               BitUtils.getField(inst, 3, 5, 1) |
                                                               BitUtils.getField(inst, 2, 2, 5), 12);
                        return op("c.jal", offset) + OP_SEP +
                               String.format("%s = pc + 2, pc += %d", reg(1), offset);
                    }
                    case 0b010: {
                        final int rd = BitUtils.getField(inst, 7, 11, 0);
                        if (rd != 0) {

                            final int imm = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 5) |
                                                                BitUtils.getField(inst, 2, 6, 0), 6);
                            return op("c.li", reg(rd), imm) + OP_SEP +
                                   String.format("%s = %d", reg(rd), imm);
                        } else {
                            return HINT;
                        }
                    }
                    case 0b011: {
                        final int rd = BitUtils.getField(inst, 7, 11, 0);
                        if (rd == 2) {

                            final int imm = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 9) |
                                                                BitUtils.getField(inst, 6, 6, 4) |
                                                                BitUtils.getField(inst, 5, 5, 6) |
                                                                BitUtils.getField(inst, 3, 4, 7) |
                                                                BitUtils.getField(inst, 2, 2, 5), 10);
                            if (imm == 0) {
                                return ILLEGAL_INSTRUCTION;
                            }
                            return op("c.addi16sp", reg(rd), imm) + OP_SEP +
                                   String.format("%s += %d", reg(rd), imm);
                        } else if (rd != 0) {

                            final int imm = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 17) |
                                                                BitUtils.getField(inst, 2, 6, 12), 18);
                            if (imm == 0) {
                                return ILLEGAL_INSTRUCTION;
                            }
                            return op("c.lui", reg(rd), imm) + OP_SEP +
                                   String.format("%s = %x", reg(rd), imm);
                        } else {
                            return HINT;
                        }
                    }
                    case 0b100: {
                        final int funct2 = BitUtils.getField(inst, 10, 11, 0);
                        final int rd = BitUtils.getField(inst, 7, 9, 0) + 8;
                        switch (funct2) {
                            case 0b00:
                            case 0b01: {
                                final int imm = BitUtils.getField(inst, 12, 12, 5) |
                                                BitUtils.getField(inst, 2, 6, 0);
                                if ((funct2 & 0b1) == 0) {
                                    return op("c.srli", reg(rd), imm) + OP_SEP +
                                           String.format("%s = %s >>> %d", reg(rd), reg(rd), imm);
                                } else {
                                    return op("c.srai", reg(rd), imm) + OP_SEP +
                                           String.format("%s = %s >> %d", reg(rd), reg(rd), imm);
                                }
                            }
                            case 0b10: { // C.ANDI

                                final int imm = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 5) |
                                                                    BitUtils.getField(inst, 2, 6, 0), 6);
                                return op("c.andi", reg(rd), imm) + OP_SEP +
                                       String.format("%s &= %d", reg(rd), imm);
                            }
                            case 0b11: {
                                final int funct3b = BitUtils.getField(inst, 5, 6, 0) |
                                                    BitUtils.getField(inst, 12, 12, 2);
                                final int rs2 = BitUtils.getField(inst, 2, 4, 0) + 8;
                                switch (funct3b) {
                                    case 0b000: {
                                        return op("c.sub", reg(rd), reg(rs2)) + OP_SEP +
                                               String.format("%s -= %s", reg(rd), reg(rs2));
                                    }
                                    case 0b001: {
                                        return op("c.xor", reg(rd), reg(rs2)) + OP_SEP +
                                               String.format("%s ^= %s", reg(rd), reg(rs2));
                                    }
                                    case 0b010: {
                                        return op("c.or", reg(rd), reg(rs2)) + OP_SEP +
                                               String.format("%s |= %s", reg(rd), reg(rs2));
                                    }
                                    case 0b011: {
                                        return op("c.and", reg(rd), reg(rs2)) + OP_SEP +
                                               String.format("%s &= %s", reg(rd), reg(rs2));
                                    }
                                    // 0b100: C.SUBW
                                    // 0b101: C.ADDW
                                }
                                break;
                            }
                        }

                        break;
                    }
                    case 0b101: {

                        final int offset = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 11) |
                                                               BitUtils.getField(inst, 11, 11, 4) |
                                                               BitUtils.getField(inst, 9, 10, 8) |
                                                               BitUtils.getField(inst, 8, 8, 10) |
                                                               BitUtils.getField(inst, 7, 7, 6) |
                                                               BitUtils.getField(inst, 6, 6, 7) |
                                                               BitUtils.getField(inst, 3, 5, 1) |
                                                               BitUtils.getField(inst, 2, 2, 5), 12);
                        return op("c.j", offset) + OP_SEP +
                               String.format("pc += %d", offset);
                    }
                    case 0b110:
                    case 0b111: {

                        final int offset = BitUtils.extendSign(BitUtils.getField(inst, 12, 12, 8) |
                                                               BitUtils.getField(inst, 10, 11, 3) |
                                                               BitUtils.getField(inst, 5, 6, 6) |
                                                               BitUtils.getField(inst, 3, 4, 1) |
                                                               BitUtils.getField(inst, 2, 2, 5), 9);
                        final int rs1 = BitUtils.getField(inst, 7, 9, 0) + 8;
                        if ((funct3 & 0b1) == 0) {
                            return op("c.beqz", reg(rs1), offset) + OP_SEP +
                                   String.format("if (%s == 0) pc += %d", reg(rs1), offset);
                        } else {
                            return op("c.bnez", reg(rs1), offset) + OP_SEP +
                                   String.format("if (%s != 0) pc += %d", reg(rs1), offset);
                        }
                    }
                }

                break;
            }

            case 0b10: { // Quadrant 2
                final int funct3 = BitUtils.getField(inst, 13, 15, 0);
                final int rd = BitUtils.getField(inst, 7, 11, 0);
                switch (funct3) {
                    case 0b000: {
                        final int imm = BitUtils.getField(inst, 12, 12, 5) |
                                        BitUtils.getField(inst, 2, 6, 0);
                        if (rd != 0) {
                            return op("c.slli", reg(rd), imm) + OP_SEP +
                                   String.format("%s = %s << %d", reg(rd), reg(rd), imm);
                        } else {
                            return HINT;
                        }
                    }
                    // 0b001: C.FLDSP
                    case 0b010: {
                        final int offset = BitUtils.getField(inst, 12, 12, 5) |
                                           BitUtils.getField(inst, 4, 6, 2) |
                                           BitUtils.getField(inst, 2, 3, 6);
                        if (rd == 0) {
                            return ILLEGAL_INSTRUCTION;
                        }

                        return op("c.lwsp", reg(rd), offset) + OP_SEP +
                               String.format("%s = *(uint32*)(%s + %d)", reg(rd), reg(2), offset);
                    }
                    // 0b011: C.FLWSP
                    case 0b100: {
                        final int rs2 = BitUtils.getField(inst, 2, 6, 0);
                        if ((inst & (1 << 12)) == 0) {
                            if (rs2 == 0) {
                                if (rd == 0) {
                                    return ILLEGAL_INSTRUCTION;
                                }

                                return op("c.jr", reg(rd)) + OP_SEP +
                                       String.format("pc = %s", reg(rd));
                            } else {
                                if (rd != 0) {
                                    return op("c.mv", reg(rd), reg(rs2)) + OP_SEP +
                                           String.format("%s = %s", reg(rd), reg(rs2));
                                } else {
                                    return HINT;
                                }
                            }
                        } else {
                            if (rs2 == 0) {
                                if (rd == 0) {
                                    return op("c.ebreak");
                                } else {
                                    return op("c.jalr", reg(rd)) + OP_SEP +
                                           String.format("%s = pc + 2, pc + %s", reg(1), reg(rd));
                                }
                            } else {
                                if (rd != 0) {
                                    return op("c.add", reg(rd), reg(rs2)) + OP_SEP +
                                           String.format("%s += %s", reg(rd), reg(rs2));
                                } else {
                                    return HINT;
                                }
                            }
                        }
                    }
                    // 0b101: C.FSDSP
                    case 0b110: {
                        final int offset = BitUtils.getField(inst, 9, 12, 2) |
                                           BitUtils.getField(inst, 7, 8, 6);
                        final int rs2 = BitUtils.getField(inst, 2, 6, 0);
                        return op("c.swsp", reg(rs2), offset) + OP_SEP +
                               String.format("*(uint32*)(%s + %d) = %s", reg(2), offset, reg(rs2));
                    }
                    // 0b111: C.FSWSP
                }

                break;
            }
        }

        return ILLEGAL_INSTRUCTION;
    }

    private static String op(final Object... args) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            if (i > 1) sb.append(", ");
            else if (i == 1) sb.append("\t");
            if (arg instanceof Integer) {
                sb.append(String.format("0x%x", arg));
            } else {
                sb.append(arg);
            }
        }

        return sb.toString();
    }

    private static String addr(final int base, final int offset) {
        return String.format("%d(%s)", offset, reg(base));
    }

    private static String reg(final Object index) {
        if (index instanceof Integer) {
            switch ((int) index) {
                case 0:
                    return "zero";
                case 1:
                    return "ra";
                case 2:
                    return "sp";
                case 3:
                    return "gp";
                case 4:
                    return "tp";
                case 5:
                    return "t0";
                case 6:
                    return "t1";
                case 7:
                    return "t2";
                case 8:
                    return "s0";
                case 9:
                    return "s1";
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                    return "a" + ((int) index - 10);
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                    return "s" + ((int) index - 16);
                case 28:
                case 29:
                case 30:
                case 31:
                    return "t" + ((int) index - 25);
                default:
                    return "?";
            }
        }

        return index.toString();
    }

    private static String csr2n(final int csr) {
        switch (csr) {
            case 0x000:
                return "ustatus";
            case 0x004:
                return "uie";
            case 0x005:
                return "utvec";

            case 0x040:
                return "uscratch";
            case 0x041:
                return "uepc";
            case 0x042:
                return "ucause";
            case 0x043:
                return "utval";
            case 0x044:
                return "uip";

            case 0x001:
                return "fflags";
            case 0x002:
                return "frm";
            case 0x003:
                return "fcsr";

            case 0xC00:
                return "cycle";
            case 0xC01:
                return "time";
            case 0xC02:
                return "instret";

            case 0xC80:
                return "cycleh";
            case 0xC81:
                return "timeh";
            case 0xC82:
                return "instreth";

            case 0x100:
                return "sstatus";
            case 0x102:
                return "sedeleg";
            case 0x103:
                return "sideleg";
            case 0x104:
                return "sie";
            case 0x105:
                return "stvec";
            case 0x106:
                return "scountern";

            case 0x140:
                return "sscratch";
            case 0x141:
                return "sepc";
            case 0x142:
                return "scause";
            case 0x143:
                return "stval";
            case 0x144:
                return "sip";

            case 0x180:
                return "satp";

            case 0x600:
                return "hstatus";
            case 0x602:
                return "hedeleg";
            case 0x603:
                return "hideleg";
            case 0x604:
                return "hie";
            case 0x606:
                return "hcounteren";
            case 0x607:
                return "hgeie";

            case 0x643:
                return "htval";
            case 0x644:
                return "hip";
            case 0x645:
                return "hvip";
            case 0x64A:
                return "htinst";
            case 0xE12:
                return "hgeip";

            case 0x680:
                return "hgatp";

            case 0x605:
                return "htimedelta";
            case 0x615:
                return "htimedeltah";

            case 0x200:
                return "vsstatus";
            case 0x204:
                return "vsie";
            case 0x205:
                return "vstvec";
            case 0x240:
                return "vsscratch";
            case 0x241:
                return "vsepc";
            case 0x242:
                return "vscause";
            case 0x243:
                return "vstval";
            case 0x244:
                return "vsip";
            case 0x280:
                return "vsatp";

            case 0xF11:
                return "mvendorid";
            case 0xF12:
                return "marchid";
            case 0xF13:
                return "mimpid";
            case 0xF14:
                return "mhartid";

            case 0x300:
                return "mstatus";
            case 0x301:
                return "misa";
            case 0x302:
                return "medeleg";
            case 0x303:
                return "mideleg";
            case 0x304:
                return "mie";
            case 0x305:
                return "mtvec";
            case 0x306:
                return "mcounteren";
            case 0x310:
                return "mstatush";

            case 0x340:
                return "mscratch";
            case 0x341:
                return "mepc";
            case 0x342:
                return "mcause";
            case 0x343:
                return "mtval";
            case 0x344:
                return "mip";
            case 0x34A:
                return "mtinst";
            case 0x34B:
                return "mtval2";

            case 0xB00:
                return "mcycle";
            case 0xB02:
                return "minstret";
            case 0xB80:
                return "mcycleh";
            case 0xB82:
                return "minstreth";

            case 0x320:
                return "mcounterhibit";

            case 0x7A0:
                return "tselect";
            case 0x7A1:
                return "tdata1";
            case 0x7A2:
                return "tdata2";
            case 0x7A3:
                return "tdata3";

            case 0x7B0:
                return "dcsr";
            case 0x7B1:
                return "dpc";
            case 0x7B2:
                return "dscratch0";
            case 0x7B3:
                return "dscratch1";
        }

        if (csr >= 0xC03 && csr <= 0xC1F) {
            return "hpmcounter" + (3 + (csr - 0xC03));
        }

        if (csr >= 0xC83 && csr <= 0xC9F) {
            return "hpmcounter" + (3 + (csr - 0xC03)) + "h";
        }

        if (csr >= 0x3A0 && csr <= 0x3AF) {
            return "pmpcfg" + (csr - 0x3A0);
        }
        if (csr >= 0x3B0 && csr <= 0x3EF) {
            return "pmpaddr" + (csr - 0x3B0);
        }

        if (csr >= 0xB03 && csr <= 0xB1F) {
            return "mhpmcounter" + (3 + (csr - 0xB03));
        }
        if (csr >= 0xB83 && csr <= 0xB9F) {
            return "mhpmcounter" + (3 + (csr - 0xB83)) + "h";
        }

        if (csr >= 0x323 && csr <= 0x33F) {
            return "mhpmevent" + (3 + (csr - 0x323));
        }

        return String.valueOf(csr);
    }
}
