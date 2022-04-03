package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.SessionLayer;
import li.cil.oc2.api.inet.SessionLayerInternetProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DefaultInternetProvider extends SessionLayerInternetProvider {
    public static final DefaultInternetProvider INSTANCE = new DefaultInternetProvider();

    private DefaultInternetProvider() {
    }

    @Override
    protected SessionLayer provideSessionLayer(final LayerParameters layerParameters) {
        return new DefaultSessionLayer(layerParameters);
    }
}
