package li.cil.oc2.api.inet;

import li.cil.oc2.common.inet.DefaultSessionLayer;
import li.cil.oc2.common.inet.DefaultTransportLayer;
import li.cil.oc2.common.inet.InetUtils;
import li.cil.oc2.common.inet.NullLayer;

/**
 * An {@link InternetProvider} partial implementation that expects an {@link SessionLayer} implementation from
 * protected method {@link SessionLayerInternetProvider#provideSessionLayer(LayerParameters)}.
 *
 * @see InternetProvider
 * @see SessionLayer
 */
public abstract class SessionLayerInternetProvider extends TransportLayerInternetProvider {

    protected static SessionLayer nullSessionLayer() {
        return NullLayer.INSTANCE;
    }

    protected static SessionLayer defaultSessionLayer(final LayerParameters layerParameters) {
        return new DefaultSessionLayer(layerParameters);
    }

    /**
     * This method is called from {@link SessionLayerInternetProvider#provideTransportLayer(LayerParameters)} in order to get a
     * {@link SessionLayer} implementation.
     * Retrieved {@link SessionLayer} implementation will be wrapped with internal {@link TransportLayer}
     * implementation.
     *
     * @return an implementation of session TCP/IP layer for internet cards
     */
    protected abstract SessionLayer provideSessionLayer(LayerParameters layerParameters);

    @Override
    protected final TransportLayer provideTransportLayer(final LayerParameters layerParameters) {
        final LayerParameters sessionParameters = InetUtils.nextLayerParameters(layerParameters, "Session");
        final SessionLayer sessionLayer = provideSessionLayer(sessionParameters);
        return InetUtils.createLayerIfNotStub(sessionLayer, DefaultTransportLayer::new);
    }
}
