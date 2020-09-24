package li.cil.circuity.vm.device.virtio;

/**
 * This exception is thrown in a VirtIO device when its internal state gets in
 * an error state. Implementations of VirtIO devices should stop whatever they
 * are doing when encountering such an exception and wait for a reset.
 */
public final class VirtIODeviceException extends Exception {
}
