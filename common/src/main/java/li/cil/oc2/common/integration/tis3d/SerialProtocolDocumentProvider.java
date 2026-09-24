/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.tis3d;

import dev.architectury.registry.registries.RegistrarManager;
import li.cil.manual.api.ManualModel;
import li.cil.manual.api.prefab.provider.NamespaceDocumentProvider;
import li.cil.manual.api.util.Constants;
import li.cil.manual.api.util.MatchResult;
import li.cil.oc2.api.API;
import li.cil.oc2.common.integration.ModIntegration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SerialProtocolDocumentProvider extends NamespaceDocumentProvider {
    public SerialProtocolDocumentProvider() {
        super(API.MOD_ID, "doc/" + ModIntegration.TIS3D.modId());
    }

    @Override
    public MatchResult matches(final ManualModel manual) {
        final var manualRegistry = RegistrarManager.get(Constants.MOD_ID).get(Constants.MANUAL_REGISTRY);
        final var manualNamespace = manualRegistry.getKey(manual).map(key -> key.location().getNamespace());
        final var isRightManual = manualNamespace.map(namespace -> namespace.equals(ModIntegration.TIS3D.modId()));
        return isRightManual.orElse(false) ? MatchResult.MATCH : MatchResult.MISMATCH;
    }
}
