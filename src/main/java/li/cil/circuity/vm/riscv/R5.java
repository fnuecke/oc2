package li.cil.circuity.vm.riscv;

@SuppressWarnings({"unused", "RedundantSuppression", "PointlessBitwiseExpression"})
public final class R5 {
    // Privilege levels.
    public static final int PRIVILEGE_U = 0; // User
    public static final int PRIVILEGE_S = 1; // Supervisor
    public static final int PRIVILEGE_H = 2; // Hypervisor
    public static final int PRIVILEGE_M = 3; // Machine

    // Software interrupts.
    public static final int USIP_SHIFT = 0; // User
    public static final int SSIP_SHIFT = 1; // Supervisor
    public static final int HSIP_SHIFT = 2; // Hypervisor
    public static final int MSIP_SHIFT = 3; // Machine

    // Timer interrupts.
    public static final int UTIP_SHIFT = 4; // User
    public static final int STIP_SHIFT = 5; // Supervisor
    public static final int HTIP_SHIFT = 6; // Hypervisor
    public static final int MTIP_SHIFT = 7; // Machine

    // External interrupts.
    public static final int UEIP_SHIFT = 8; // User
    public static final int SEIP_SHIFT = 9; // Supervisor
    public static final int HEIP_SHIFT = 10; // Hypervisor
    public static final int MEIP_SHIFT = 11; // Machine

    // Interrupt masks for mip/mideleg CSRs.
    public static final int USIP_MASK = 0b1 << USIP_SHIFT;
    public static final int SSIP_MASK = 0b1 << SSIP_SHIFT;
    public static final int HSIP_MASK = 0b1 << HSIP_SHIFT;
    public static final int MSIP_MASK = 0b1 << MSIP_SHIFT;
    public static final int UTIP_MASK = 0b1 << UTIP_SHIFT;
    public static final int STIP_MASK = 0b1 << STIP_SHIFT;
    public static final int HTIP_MASK = 0b1 << HTIP_SHIFT;
    public static final int MTIP_MASK = 0b1 << MTIP_SHIFT;
    public static final int UEIP_MASK = 0b1 << UEIP_SHIFT;
    public static final int SEIP_MASK = 0b1 << SEIP_SHIFT;
    public static final int HEIP_MASK = 0b1 << HEIP_SHIFT;
    public static final int MEIP_MASK = 0b1 << MEIP_SHIFT;

    // Machine status (mstatus[h]) CSR masks and offsets.
    public static final int STATUS_UIE_SHIFT = 0; // U-mode interrupt-enable bit
    public static final int STATUS_SIE_SHIFT = 1; // S-mode interrupt-enable bit
    public static final int STATUS_MIE_SHIFT = 3; // M-mode interrupt-enable bit
    public static final int STATUS_UPIE_SHIFT = 4; // Prior U-mode interrupt-enabled bit.
    public static final int STATUS_SPIE_SHIFT = 5; // Prior S-mode interrupt-enabled bit.
    public static final int STATUS_UBE_SHIFT = 6; // U-mode fetch/store endianness (0 = little, 1 = big).
    public static final int STATUS_MPIE_SHIFT = 7; // Prior M-mode interrupt-enabled bit.
    public static final int STATUS_SPP_SHIFT = 8; // Prior S-mode privilege mode.
    public static final int STATUS_MPP_SHIFT = 11; // Prior M-mode privilege mode.
    public static final int STATUS_FS_SHIFT = 13;
    public static final int STATUS_XS_SHIFT = 15;
    public static final int STATUS_MPRV_SHIFT = 17; // Modify PRiVilege.
    public static final int STATUS_SUM_SHIFT = 18; // Permit Supervisor User Memory access.
    public static final int STATUS_MXR_SHIFT = 19; // Make eXecutable Readable.
    public static final int STATUS_TVM_SHIFT = 20; // Trap Virtual Memory
    public static final int STATUS_TW_SHIFT = 21; // Timeout Wait
    public static final int STATUS_TSR_SHIFT = 22; // Trap SRET
    public static final int STATUS_SD_SHIFT = 31; // State Dirty
    public static final int STATUSH_GVA_SHIFT = 6; // Guest Virtual Address
    public static final int STATUSH_MPV_SHIFT = 6; // Machine Previous Virtualization Mode

    public static final int STATUS_UIE_MASK = 1 << STATUS_UIE_SHIFT;
    public static final int STATUS_SIE_MASK = 1 << STATUS_SIE_SHIFT;
    public static final int STATUS_MIE_MASK = 1 << STATUS_MIE_SHIFT;
    public static final int STATUS_UPIE_MASK = 1 << STATUS_UPIE_SHIFT;
    public static final int STATUS_SPIE_MASK = 1 << STATUS_SPIE_SHIFT;
    public static final int STATUS_UBE_MASK = 1 << STATUS_UBE_SHIFT;
    public static final int STATUS_MPIE_MASK = 1 << STATUS_MPIE_SHIFT;
    public static final int STATUS_SPP_MASK = 1 << STATUS_SPP_SHIFT;
    public static final int STATUS_MPP_MASK = 0b11 << STATUS_MPP_SHIFT;
    public static final int STATUS_FS_MASK = 0b11 << STATUS_FS_SHIFT;
    public static final int STATUS_XS_MASK = 0b11 << STATUS_XS_SHIFT;
    public static final int STATUS_MPRV_MASK = 1 << STATUS_MPRV_SHIFT;
    public static final int STATUS_SUM_MASK = 1 << STATUS_SUM_SHIFT;
    public static final int STATUS_MXR_MASK = 1 << STATUS_MXR_SHIFT;
    public static final int STATUS_TVM_MASK = 1 << STATUS_TVM_SHIFT;
    public static final int STATUS_TW_MASK = 1 << STATUS_TW_SHIFT;
    public static final int STATUS_TSR_MASK = 1 << STATUS_TSR_SHIFT;
    public static final int STATUS_SD_MASK = 1 << STATUS_SD_SHIFT;
    public static final int STATUSH_GVA_MASK = 1 << STATUSH_GVA_SHIFT;
    public static final int STATUSH_MPV_MASK = 1 << STATUSH_MPV_SHIFT;

    // Exception codes used mep/medeleg CSRs.
    public static final int EXCEPTION_MISALIGNED_FETCH = 0;
    public static final int EXCEPTION_FAULT_FETCH = 1;
    public static final int EXCEPTION_ILLEGAL_INSTRUCTION = 2;
    public static final int EXCEPTION_BREAKPOINT = 3;
    public static final int EXCEPTION_MISALIGNED_LOAD = 4;
    public static final int EXCEPTION_FAULT_LOAD = 5;
    public static final int EXCEPTION_MISALIGNED_STORE = 6;
    public static final int EXCEPTION_FAULT_STORE = 7;
    public static final int EXCEPTION_USER_ECALL = 8;
    public static final int EXCEPTION_SUPERVISOR_ECALL = 9;
    public static final int EXCEPTION_HYPERVISOR_ECALL = 10;
    public static final int EXCEPTION_MACHINE_ECALL = 11;
    public static final int EXCEPTION_FETCH_PAGE_FAULT = 12;
    public static final int EXCEPTION_LOAD_PAGE_FAULT = 13;
    public static final int EXCEPTION_STORE_PAGE_FAULT = 15;

    // Highest bit means it's an interrupt/asynchronous exception, otherwise a regular exception.
    public static final int INTERRUPT = 1 << 31;

    // Supported counters in [m|s]counteren CSRs.
    public static final int MCOUNTERN_CY = 1 << 0;
    public static final int MCOUNTERN_TM = 1 << 1;
    public static final int MCOUNTERN_IR = 1 << 2;
    public static final int MCOUNTERN_HPM3 = 1 << 3; // Contiguous HPM counters up to HPM31 after this.

    // SATP CSR masks.
    public static final int SATP_PPN_MASK = 0b0000_0000_0011_1111_1111_1111_1111_1111;
    public static final int SATP_ASID_MASK = 0b0111_1111_1100_0000_0000_0000_0000_0000;
    public static final int SATP_MODE_MASK = 0b1000_0000_0000_0000_0000_0000_0000_0000;

    // Page sizes are 4KiB (V2p73).
    public static final int PAGE_ADDRESS_SHIFT = 12; // 1<<12 == 4096; SATP << 12 == root PTE address
    public static final int PAGE_ADDRESS_MASK = (1 << PAGE_ADDRESS_SHIFT) - 1;

    // Page table entry masks. See V2p73.
    public static final int PTE_DATA_BITS = 10; // Number of PTE data bits.
    public static final int PTE_V_MASK = 0b1 << 0; // Valid flag.
    public static final int PTE_R_MASK = 0b1 << 1; // Allow read access.
    public static final int PTE_W_MASK = 0b1 << 2; // Allow write access.
    public static final int PTE_X_MASK = 0b1 << 3; // Allow code execution (instruction fetch).
    public static final int PTE_U_MASK = 0b1 << 4; // Allow access to user mode only.
    public static final int PTE_G_MASK = 0b1 << 5; // Global mapping.
    public static final int PTE_A_MASK = 0b1 << 6; // Accessed flag (read, written or fetched).
    public static final int PTE_D_MASK = 0b1 << 7; // Dirty flag (written).
    public static final int PTE_RSW_MASK = 0b11 << 8; // Reserved for supervisor software.

    // Config for SV32 configuration.
    public static final int SV32_LEVELS = 2;
    public static final int SV32_PTE_SIZE_LOG2 = 2; // => * size == << log2(size)
    public static final int SV32_XPN_SIZE = 10; // page number size per level in bits
    public static final int SV32_XPN_MASK = (1 << SV32_XPN_SIZE) - 1;

    // Floating point extension CSR.
    public static final int FCSR_FFLAGS_NX_MASK = 0b1 << 0; // Inexact.
    public static final int FCSR_FFLAGS_UF_MASK = 0b1 << 1; // Underflow.
    public static final int FCSR_FFLAGS_OF_MASK = 0b1 << 2; // Overflow.
    public static final int FCSR_FFLAGS_DZ_MASK = 0b1 << 3; // Division by zero.
    public static final int FCSR_FFLAGS_NV_MASK = 0b1 << 4; // Invalid operation.
    public static final int FCSR_FRM_SHIFT = 5;
    public static final int FCSR_FFLAGS_MASK = 0b11111;
    public static final int FCSR_FRM_MASK = 0b111 << FCSR_FRM_SHIFT;

    // Floating point rounding modes.
    public static final int FCSR_FRM_RNE = 0b000; // Round to nearest, ties to even.
    public static final int FCSR_FRM_RTZ = 0b001; // Round towards zero.
    public static final int FCSR_FRM_RDN = 0b010; // Round down (towards negative infinity).
    public static final int FCSR_FRM_RUP = 0b011; // Round up (towards positive infinity).
    public static final int FCSR_FRM_RMM = 0b100; // Round to nearest, ties to max magnitude.
    public static final int FCSR_FRM_DYN = 0b111; // Use rm field of instruction to determine rounding mode.

    /**
     * Computes flags for the machine ISA CSR given a list of extension letters.
     *
     * @param extensions the list of extensions to build a mask from.
     * @return the mask representing the list of extensions.
     */
    public static int isa(final char... extensions) {
        int result = 0;
        for (final char ch : extensions) {
            final char extension = Character.toUpperCase(ch);
            if (extension < 'A' || extension > 'Z') {
                throw new IllegalArgumentException("Not a valid extension letter: " + extension);
            }

            result |= 1 << (extension - 'A');
        }
        return result;
    }

    /**
     * Gets the value for the MXL field in the misa CSR for a given XLEN.
     *
     * @param xlen the XLEN to get the MXL value for. Must be 32, 64 or 128.
     * @return the MXL for the specified XLEN.
     */
    public static int mxl(final int xlen) {
        switch (xlen) {
            case 32:
                return 0b01;
            case 64:
                return 0b10;
            case 128:
                return 0b11;
            default:
                throw new IllegalArgumentException();
        }
    }
}
