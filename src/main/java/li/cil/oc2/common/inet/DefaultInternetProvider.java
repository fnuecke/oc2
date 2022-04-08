package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.provider.SessionLayerInternetProvider;

public final class DefaultInternetProvider extends SessionLayerInternetProvider {
    public static final DefaultInternetProvider INSTANCE = new DefaultInternetProvider();

    private DefaultInternetProvider() {
    }

    @Override
    protected SessionLayer provideSessionLayer(final LayerParameters layerParameters) {
        return new DefaultSessionLayer(layerParameters);
    }
}
