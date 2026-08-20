/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.platform;

/**
 * Fabric specific interface, used to define an entrypoint that is invoked once all registries created
 * by the mod exist and entries may be registered with them. This is Fabric's approach to allowing
 * ordered initialization. For NeoForge this is not required, since it allows declaring mod
 * initialization ordering.
 * <p>
 * Declare an implementation under the {@code oc2:registration} entrypoint in {@code fabric.mod.json}:
 * <pre>
 * "entrypoints": {
 *     "oc2:registration": [
 *         "com.example.MyRegistrationInitializer"
 *     ]
 * }
 * </pre>
 *
 * @see li.cil.oc2.api.util.Registries
 */
public interface FabricRegistrationInitializer {
    /**
     * Registers objects with registries created by the mod.
     */
    void registerObjects();
}
