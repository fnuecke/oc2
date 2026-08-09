/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

/**
 * This interface is used to declare callback documentation on targets of an {@link ObjectDevice}.
 * <p>
 * It provides an alternative to declaring documentation to specifying the documentation directly
 * in the {@link Callback} and {@link Parameter} annotations. This may be preferable to some as
 * it reduces the visual clutter around exposed methods.
 * <p>
 * For less fragile code it is recommended to explicitly specify the callback name via
 * {@link Callback#name()} and use a constant for both the declaration and when adding
 * documentation via {@link DeviceVisitor#visitCallback(String)}. This is omitted in
 * the example for brevity.
 * <p>
 * Example:
 * <pre>
 * &#64;Callback
 * public void exampleMethod(&#64;Parameter("a") int a) {
 *     // ...
 * }
 *
 * &#64;Override
 * public void getDeviceDocumentation(DocumentationVisitor visitor) {
 *     visitor.visitCallback("exampleMethod")
 *         .description("This is an example method.")
 *         .parameterDescription("a", "This is a test parameter.");
 * }
 * </pre>
 */
public interface DocumentedDevice {
    /**
     * Called when the class is first scanned for annotated methods.
     * <p>
     * The results of this method are cached.
     *
     * @param visitor an object that allows declaring documentation for this class.
     */
    void getDeviceDocumentation(DeviceVisitor visitor);

    /**
     * Provides facilities for declaring class level documentation.
     */
    interface DeviceVisitor {
        /**
         * Obtain a visitor for a named callback.
         * <p>
         * The name must match the name of the method annotated as {@link Callback}
         * to associate the documentation provided to the returned {@link CallbackVisitor}
         * with it. The name of the method is the name explicitly specified in the
         * {@link Callback} annotation, or the name of the method, if left blank.
         *
         * @param callbackName the name of the callback to add documentation for.
         * @return a documentation visitor for the specified callback.
         */
        CallbackVisitor visitCallback(String callbackName);
    }

    /**
     * Provides facilities for declaring callback level documentation.
     */
    interface CallbackVisitor {
        /**
         * Sets the description of the callback.
         * <p>
         * This corresponds to {@link Callback#description()}.
         *
         * @param value the description.
         * @return this visitor to allow call chaining.
         */
        CallbackVisitor description(String value);

        /**
         * Sets the return value description of this callback.
         * <p>
         * This corresponds to {@link Callback#returnValueDescription()}.
         *
         * @param value the return value description.
         * @return this visitor to allow call chaining.
         */
        CallbackVisitor returnValueDescription(String value);

        /**
         * Sets the description for the specified parameter.
         * <p>
         * This will only work for parameters that have been annotated with
         * {@link Parameter}. The specified name must match that defined in
         * the annotation, i.e. {@link Parameter#value()}.
         * <p>
         * This corresponds to {@link Parameter#description()}.
         *
         * @param parameterName the name of the parameter to set the description for.
         * @param value         the description.
         * @return this visitor to allow call chaining.
         */
        CallbackVisitor parameterDescription(String parameterName, String value);
    }
}
