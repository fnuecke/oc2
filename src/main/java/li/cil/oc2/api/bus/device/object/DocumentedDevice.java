package li.cil.oc2.api.bus.device.object;

/**
 * This interface is used to declare callback documentation on targets of an {@link ObjectDevice}.
 * <p>
 * It provides an alternative to declaring documentation to specifying the documentation directly
 * in the {@link Callback} and {@link Parameter} annotations. This may be preferable to some as
 * it reduces the visual clutter around exposed methods.
 */
public interface DocumentedDevice {
    void getDeviceDocumentation(DocumentationVisitor visitor);

    interface DocumentationVisitor {
        CallbackVisitor visitCallback(String callbackName);
    }

    interface CallbackVisitor {
        CallbackVisitor description(String value);

        CallbackVisitor returnValueDescription(String value);

        CallbackVisitor parameterDescription(String parameterName, String value);
    }
}
