package li.cil.circuity.vm.riscv;

import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

import java.util.*;
import java.util.function.Function;

public final class R5Assembler {
    private static final Map<String, InstructionSpec> INSTRUCTION_SPEC_MAP = new HashMap<>();
    private static final Map<String, String> TOKEN_ALIASES = new HashMap<>();
    private static final List<String> FIELD_ARGUMENT_ORDER = new ArrayList<>();

    static {
        new InstructionSpec("LUI", "immU;rd;opcode")
                .bind("opcode", 0b0110111)
                .postprocess("imm", i -> i >> 12)
                .register();
        new InstructionSpec("AUIPC", "immU;rd;opcode")
                .bind("opcode", 0b0010111)
                .register();

        new InstructionSpec("JAL", "immJ;rd;opcode")
                .bind("opcode", 0b1101111)
                .postprocess("imm", i -> i >> 1)
                .register();
        new InstructionSpec("JALR", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b1100111)
                .bind("funct3", 0b000)
                .register();

        new InstructionSpec("BEQ", "immB;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b1100011)
                .bind("funct3", 0b000)
                .register();
        new InstructionSpec("BNE", "immB;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b1100011)
                .bind("funct3", 0b001)
                .register();
        new InstructionSpec("BLT", "immB;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b1100011)
                .bind("funct3", 0b100)
                .register();
        new InstructionSpec("BGE", "immB;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b1100011)
                .bind("funct3", 0b101)
                .register();
        new InstructionSpec("BLTU", "immB;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b1100011)
                .bind("funct3", 0b110)
                .register();
        new InstructionSpec("BGEU", "immB;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b1100011)
                .bind("funct3", 0b111)
                .register();

        new InstructionSpec("LB", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0000011)
                .bind("funct3", 0b000)
                .register();
        new InstructionSpec("LH", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0000011)
                .bind("funct3", 0b001)
                .register();
        new InstructionSpec("LW", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0000011)
                .bind("funct3", 0b010)
                .register();
        new InstructionSpec("LBU", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0000011)
                .bind("funct3", 0b100)
                .register();
        new InstructionSpec("LHU", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0000011)
                .bind("funct3", 0b101)
                .register();

        new InstructionSpec("SB", "immS;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b0100011)
                .bind("funct3", 0b000)
                .register();
        new InstructionSpec("SH", "immS;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b0100011)
                .bind("funct3", 0b001)
                .register();
        new InstructionSpec("SW", "immS;rs2;rs1;funct3;opcode")
                .bind("opcode", 0b0100011)
                .bind("funct3", 0b010)
                .register();

        new InstructionSpec("ADDI", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b000)
                .register();
        new InstructionSpec("SLTI", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b010)
                .register();
        new InstructionSpec("SLTIU", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b011)
                .register();
        new InstructionSpec("XORI", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b100)
                .register();
        new InstructionSpec("ORI", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b110)
                .register();
        new InstructionSpec("ANDI", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b111)
                .register();

        new InstructionSpec("SLLI", "funct7;shamt;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b001)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SRLI", "funct7;shamt;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b101)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SRAI", "funct7;shamt;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0010011)
                .bind("funct3", 0b101)
                .bind("funct7", 0b0100000)
                .register();

        new InstructionSpec("ADD", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b000)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SUB", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b000)
                .bind("funct7", 0b0100000)
                .register();
        new InstructionSpec("SLL", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b001)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SLT", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b010)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SLTU", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b011)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("XOR", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b100)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SRL", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b101)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("SRA", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b101)
                .bind("funct7", 0b0100000)
                .register();
        new InstructionSpec("OR", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b110)
                .bind("funct7", 0b0000000)
                .register();
        new InstructionSpec("AND", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b111)
                .bind("funct7", 0b0000000)
                .register();

        new InstructionSpec("FENCE", "fm[31:27];pred[26:24];succ[23:20];rs1;funct3;rd;opcode")
                .bind("opcode", 0b0001111)
                .bind("funct3", 0b000)
                .bind("fm", 0b00000)
                .bind("pred", 0b000)
                .bind("succ", 0b0000)
                .register();
        new InstructionSpec("FENCE.I", "immI;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0001111)
                .bind("funct3", 0b001)
                .register();

        new InstructionSpec("ECALL", "funct12;zero[19:7];opcode")
                .bind("opcode", 0b1110011)
                .bind("zero", 0)
                .bind("funct12", 0b000000000000)
                .register();
        new InstructionSpec("EBREAK", "funct12;zero[19:7];opcode")
                .bind("opcode", 0b1110011)
                .bind("zero", 0)
                .bind("funct12", 0b000000000001)
                .register();

        new InstructionSpec("CSRRW", "csr;rs1;funct3;rd;opcode")
                .bind("opcode", 0b1110011)
                .bind("funct3", 0b001)
                .register();
        new InstructionSpec("CSRRS", "csr;rs1;funct3;rd;opcode")
                .bind("opcode", 0b1110011)
                .bind("funct3", 0b010)
                .register();
        new InstructionSpec("CSRRC", "csr;rs1;funct3;rd;opcode")
                .bind("opcode", 0b1110011)
                .bind("funct3", 0b011)
                .register();
        new InstructionSpec("CSRRWI", "csr;uimm;funct3;rd;opcode")
                .bind("opcode", 0b1110011)
                .bind("funct3", 0b101)
                .register();
        new InstructionSpec("CSRRSI", "csr;uimm;funct3;rd;opcode")
                .bind("opcode", 0b1110011)
                .bind("funct3", 0b110)
                .register();
        new InstructionSpec("CSRRCI", "csr;uimm;funct3;rd;opcode")
                .bind("opcode", 0b1110011)
                .bind("funct3", 0b111)
                .register();

        new InstructionSpec("MUL", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b000)
                .bind("funct7", 0b0000001)
                .register();
        new InstructionSpec("MULH", "funct12;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b001)
                .bind("funct12", 0b0000001)
                .register();
        new InstructionSpec("MULHSU", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b010)
                .bind("funct7", 0b0000001)
                .register();
        new InstructionSpec("MULHU", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b011)
                .bind("funct7", 0b0000001)
                .register();
        new InstructionSpec("DIV", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b100)
                .bind("funct7", 0b0000001)
                .register();
        new InstructionSpec("DIVU", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b101)
                .bind("funct7", 0b0000001)
                .register();
        new InstructionSpec("REM", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b110)
                .bind("funct7", 0b0000001)
                .register();
        new InstructionSpec("REMU", "funct7;rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0110011)
                .bind("funct3", 0b111)
                .bind("funct7", 0b0000001)
                .register();

        new InstructionSpec("LW.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("rs2", 0b00000)
                .bind("funct5", 0b00010)
                .register();
        new InstructionSpec("SC.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b00011)
                .register();
        new InstructionSpec("AMOSWAP.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b00001)
                .register();
        new InstructionSpec("AMOADD.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b00000)
                .register();
        new InstructionSpec("AMOXOR.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b00100)
                .register();
        new InstructionSpec("AMOAND.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b01100)
                .register();
        new InstructionSpec("AMOOR.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b01000)
                .register();
        new InstructionSpec("AMOMIN.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b10000)
                .register();
        new InstructionSpec("AMOMAX.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b10100)
                .register();
        new InstructionSpec("AMOMINU.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b11000)
                .register();
        new InstructionSpec("AMOMAXU.W", "funct5;aq[26];rl[25];rs2;rs1;funct3;rd;opcode")
                .bind("opcode", 0b0101111)
                .bind("funct3", 0b010)
                .bind("funct5", 0b11100)
                .register();

        TOKEN_ALIASES.put("zero", "x0");
        TOKEN_ALIASES.put("ra", "x1");
        TOKEN_ALIASES.put("sp", "x2");
        TOKEN_ALIASES.put("gp", "x3");
        TOKEN_ALIASES.put("tp", "x4");
        TOKEN_ALIASES.put("t0", "x5");
        TOKEN_ALIASES.put("t1", "x6");
        TOKEN_ALIASES.put("t2", "x7");
        TOKEN_ALIASES.put("s0", "x8");
        TOKEN_ALIASES.put("fp", "x8");
        TOKEN_ALIASES.put("s1", "x9");
        TOKEN_ALIASES.put("a0", "x10");
        TOKEN_ALIASES.put("a1", "x11");
        TOKEN_ALIASES.put("a2", "x12");
        TOKEN_ALIASES.put("a3", "x13");
        TOKEN_ALIASES.put("a4", "x14");
        TOKEN_ALIASES.put("a5", "x15");
        TOKEN_ALIASES.put("a6", "x16");
        TOKEN_ALIASES.put("a7", "x17");
        TOKEN_ALIASES.put("s2", "x18");
        TOKEN_ALIASES.put("s3", "x19");
        TOKEN_ALIASES.put("s4", "x20");
        TOKEN_ALIASES.put("s5", "x21");
        TOKEN_ALIASES.put("s6", "x22");
        TOKEN_ALIASES.put("s7", "x23");
        TOKEN_ALIASES.put("s8", "x24");
        TOKEN_ALIASES.put("s9", "x25");
        TOKEN_ALIASES.put("s10", "x26");
        TOKEN_ALIASES.put("s11", "x27");
        TOKEN_ALIASES.put("t3", "x28");
        TOKEN_ALIASES.put("t4", "x29");
        TOKEN_ALIASES.put("t5", "x30");
        TOKEN_ALIASES.put("t6", "x31");

        FIELD_ARGUMENT_ORDER.add("rd");
        FIELD_ARGUMENT_ORDER.add("rs1");
        FIELD_ARGUMENT_ORDER.add("rs2");
        FIELD_ARGUMENT_ORDER.add("imm");
    }

    public static void assemble(final String code, final PhysicalMemory memory, final int offset) throws MemoryAccessException {
        assemble(code.split("\n"), memory, offset);
    }

    public static void assemble(final String[] code, final PhysicalMemory memory, final int offset) throws MemoryAccessException {
        int writeIndex = offset;
        for (final String line : code) {
            String processedLine;
            final int commentStart = line.indexOf('#');
            if (commentStart >= 0) {
                processedLine = line.substring(0, commentStart).trim();
            } else {
                processedLine = line.trim();
            }

            if (processedLine.isEmpty()) {
                continue;
            }

            final String[] instAndArgs = line.trim().split(" ", 2);
            final String instName = instAndArgs[0];
            final InstructionSpec inst = INSTRUCTION_SPEC_MAP.get(instName.toLowerCase());
            if (inst == null) {
                throw new IllegalArgumentException("illegal instruction name '" + instName + "'");
            }

            final int instAssembled;
            if (instAndArgs.length > 1) {
                final String[] args = instAndArgs[1].split(",");
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
                instAssembled = inst.assemble(resolveArgs(inst.fields.keySet(), args));
            } else {
                instAssembled = inst.assemble(Collections.emptyMap());
            }

            memory.store(writeIndex, instAssembled, 2);
            writeIndex += 4;
        }
    }

    private static Map<String, Integer> resolveArgs(final Set<String> fieldNames, final String[] args) {
        final Map<String, Integer> result = new HashMap<>();
        int argIndex = 0;
        for (final String fieldName : FIELD_ARGUMENT_ORDER) {
            if (!fieldNames.contains(fieldName)) {
                continue;
            }

            String arg = args[argIndex];
            arg = TOKEN_ALIASES.getOrDefault(arg, arg);

            final int argValue;
            if (arg.charAt(0) == 'x') {
                argValue = Integer.decode(arg.substring(1));
            } else {
                argValue = Integer.decode(arg);
            }

            result.put(fieldName, argValue);

            argIndex++;
            if (argIndex >= args.length) {
                break;
            }
        }

        if (argIndex < args.length) {
            throw new IllegalArgumentException();
        }

        return result;
    }

    public static final class InstructionSpec {
        private static final char FIELD_SPEC_SEPARATOR = ';';
        private static final char BIT_SPEC_START = '[';
        private static final char BIT_SPEC_END = ']';
        private static final char BIT_SPEC_SEPARATOR = ',';
        public static final char BIT_SPEC_RANGE_SEPARATOR = ':';

        private static final Map<String, String> DEFAULT_BIT_RANGES = new HashMap<>();

        static {
            DEFAULT_BIT_RANGES.put("opcode", "opcode[6:0]");
            DEFAULT_BIT_RANGES.put("rd", "rd[11:7]");
            DEFAULT_BIT_RANGES.put("funct3", "funct3[14:12]");
            DEFAULT_BIT_RANGES.put("rs1", "rs1[19:15]");
            DEFAULT_BIT_RANGES.put("uimm", "rs1[19:15]");
            DEFAULT_BIT_RANGES.put("rs2", "rs2[24:20]");
            DEFAULT_BIT_RANGES.put("shamt", "rs2[24:20]");
            DEFAULT_BIT_RANGES.put("funct7", "funct7[31:25]");
            DEFAULT_BIT_RANGES.put("immI", "imm[31:20]");
            DEFAULT_BIT_RANGES.put("csr", "imm[31:20]");
            DEFAULT_BIT_RANGES.put("immS", "imm[31:25,11:7]");
            DEFAULT_BIT_RANGES.put("immB", "imm[31,7,30:25,11:6]");
            DEFAULT_BIT_RANGES.put("immU", "imm[31:12]");
            DEFAULT_BIT_RANGES.put("immJ", "imm[31,19:12,20,30:21]");
            DEFAULT_BIT_RANGES.put("funct5", "funct5[31:27]");
            DEFAULT_BIT_RANGES.put("funct12", "funct12[31:20]");
        }

        private final String name;
        private final Map<String, List<BitRange>> fields;
        private final Map<String, Integer> values;
        private Map<String, Function<Integer, Integer>> postprocessors;

        public InstructionSpec(final String name, final String spec) {
            // Spec grammar:
            // SPEC := FIELD_SPEC_LIST
            // FIELD_SPEC_LIST := FIELD_SPEC [ ";" FIELD_SPEC_LIST ]
            // FIELD_SPEC := FIELD_NAME "[" BIT_SPEC_LIST "]"
            // BIT_SPEC_LIST := BIT_SPEC [ "," BIT_SPEC_LIST ]
            // BIT_SPEC := 0-9 |       # Single bit indexing into opcode bits; equivalent to N:N
            //             0-9 ":" 0-9 # Bit range indexing into opcode bits

            this.name = name;
            fields = new HashMap<>();
            values = new HashMap<>();
            postprocessors = new HashMap<>();

            parseFieldSpecList(spec, fields);
        }

        public InstructionSpec bind(final String field, final int value) {
            values.put(field, value);
            return this;
        }

        public InstructionSpec postprocess(final String field, final Function<Integer, Integer> callback) {
            postprocessors.put(field, callback);
            return this;
        }

        public void register() {
            INSTRUCTION_SPEC_MAP.put(name.toLowerCase(), this);
        }

        public int assemble(final Map<String, Integer> values) {
            final Function<String, Integer> getValue = s -> {
                final int value = values.computeIfAbsent(s, key -> this.values.computeIfAbsent(key, s1 -> {
                    throw new IllegalArgumentException("missing value '" + s1 + "'");
                }));
                final Function<Integer, Integer> postprocessor = postprocessors.get(s);
                if (postprocessor != null) {
                    return postprocessor.apply(value);
                } else {
                    return value;
                }
            };

            int result = 0;
            for (final String fieldName : fields.keySet()) {
                final List<BitRange> fieldMapping = fields.get(fieldName);
                final int fieldValue = getValue.apply(fieldName);
                result |= map(fieldValue, fieldMapping);
            }
            return result;
        }

        private int map(final int value, final List<BitRange> mapping) {
            int mask = 0b1;
            int result = 0;
            for (final BitRange range : mapping) {
                final int high = range.high;
                final int low = range.low;
                for (int i = low; i <= high; i++, mask <<= 1) {
                    final int bitValue = (value & mask) != 0 ? 1 : 0;
                    result |= bitValue << i;
                }
            }
            return result;
        }

        private static void parseFieldSpecList(final String fieldSpecList, final Map<String, List<BitRange>> fields) {
            int fieldSpecStart = 0;
            int nextFieldSpecSplit = fieldSpecList.indexOf(FIELD_SPEC_SEPARATOR);
            do {
                if (nextFieldSpecSplit < 0)
                    nextFieldSpecSplit = fieldSpecList.length(); // No more split, use remainder.

                String fieldSpec = fieldSpecList.substring(fieldSpecStart, nextFieldSpecSplit);
                fieldSpec = DEFAULT_BIT_RANGES.getOrDefault(fieldSpec, fieldSpec);
                if (fieldSpec.length() == 0) {
                    continue;
                }

                final int bitSpecListStart = fieldSpec.indexOf(BIT_SPEC_START);
                if (bitSpecListStart < 0 || fieldSpec.charAt(fieldSpec.length() - 1) != BIT_SPEC_END) {
                    throw new IllegalArgumentException("no bit spec list for field '" + fieldSpec + "'");
                }

                final String fieldName = fieldSpec.substring(0, bitSpecListStart);

                final String bitSpecList = fieldSpec.substring(bitSpecListStart + 1, fieldSpec.length() - 1);
                int bitSpecStart = 0;
                if (bitSpecList.length() == 0) {
                    throw new IllegalArgumentException("empty bit spec list for field '" + fieldName + "'");
                }

                final List<BitRange> bitRanges = new ArrayList<>();
                int nextBitSpecSplit = bitSpecList.indexOf(BIT_SPEC_SEPARATOR);
                do {
                    if (nextBitSpecSplit < 0)
                        nextBitSpecSplit = bitSpecList.length(); // No more split, use remainder.

                    final String bitSpec = bitSpecList.substring(bitSpecStart, nextBitSpecSplit);
                    if (bitSpec.length() == 0) {
                        continue;
                    }

                    final int separatorIndex = bitSpec.indexOf(BIT_SPEC_RANGE_SEPARATOR);
                    if (separatorIndex < 0) {
                        final int bitIndex = Integer.decode(bitSpec);
                        bitRanges.add(new BitRange(bitIndex, bitIndex));
                    } else {
                        final int bitIndexHigh = Integer.decode(bitSpec.substring(0, separatorIndex));
                        final int bitIndexLow = Integer.decode(bitSpec.substring(separatorIndex + 1));
                        bitRanges.add(new BitRange(bitIndexHigh, bitIndexLow));
                    }

                    bitSpecStart = nextBitSpecSplit + 1;
                    nextBitSpecSplit = bitSpecList.indexOf(BIT_SPEC_SEPARATOR, bitSpecStart);
                } while (bitSpecStart < bitSpecList.length());

                if (bitRanges.size() == 0) {
                    throw new IllegalArgumentException("empty bit spec list for field '" + fieldName + "'");
                }

                Collections.reverse(bitRanges);

                fields.put(fieldName, bitRanges);

                fieldSpecStart = nextFieldSpecSplit + 1;
                nextFieldSpecSplit = fieldSpecList.indexOf(FIELD_SPEC_SEPARATOR, fieldSpecStart);
            } while (fieldSpecStart < fieldSpecList.length());
        }

        private static final class BitRange {
            public final int high, low;

            public BitRange(final int high, final int low) {
                this.high = Math.max(high, low);
                this.low = Math.min(high, low);
            }
        }
    }
}
