package li.cil.circuity.api.vm.devicetree;

import li.cil.circuity.vm.devicetree.DeviceTreeRegistry;

/**
 * <p>List of well-known device tree node properties.</p>
 * <p>Sourced Devicetree Specification 0.3, available at https://github.com/devicetree-org/devicetree-specification/releases</p>
 *
 * @see DeviceTreeRegistry
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public final class DeviceTreePropertyNames {
    public static final String COMPATIBLE = "compatible";
    public static final String MODEL = "model";
    public static final String PHANDLE = "phandle";
    public static final String STATUS = "status";
    public static final String NUM_ADDRESS_CELLS = "#address-cells";
    public static final String NUM_SIZE_CELLS = "#size-cells";
    public static final String REG = "reg";
    public static final String VIRTUAL_REG = "virtual-reg";
    public static final String RANGES = "ranges";
    public static final String DMA_RANGES = "dma-ranges";
    public static final String DEVICE_TYPE = "device_type";
    public static final String INTERRUPTS = "interrupts";
    public static final String INTERRUPT_PARENT = "interrupt-parent";
    public static final String INTERRUPTS_EXTENDED = "interrupts-extended";
    public static final String NUM_INTERRUPT_CELLS = "#interrupt-cells";
    public static final String INTERRUPT_CONTROLLER = "interrupt-controller";
    public static final String INTERRUPT_MAP = "interrupt-map";
    public static final String INTERRUPT_MAP_MASK = "interrupt-map-mask";
}
