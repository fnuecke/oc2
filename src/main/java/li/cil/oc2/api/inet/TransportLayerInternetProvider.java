package li.cil.oc2.api.inet;

import li.cil.oc2.common.inet.DefaultNetworkLayer;
import li.cil.oc2.common.inet.DefaultTransportLayer;
import li.cil.oc2.common.inet.InetUtils;
import li.cil.oc2.common.inet.NullLayer;

/**
 * An {@link InternetProvider} partial implementation that expects an {@link TransportLayer} implementation from
 * protected method {@link TransportLayerInternetProvider#provideTransportLayer(LayerParameters)}.
 *
 * @see InternetProvider
 * @see TransportLayer
 */
public abstract class TransportLayerInternetProvider extends NetworkLayerInternetProvider {

    protected static TransportLayer nullTransportLayer() {
        return NullLayer.INSTANCE;
    }

    protected static TransportLayer defaultTransportLayer(final LayerParameters layerParameters) {
        return new DefaultTransportLayer(SessionLayerInternetProvider.defaultSessionLayer(layerParameters));
    }

    /**
     * This method is called from {@link TransportLayerInternetProvider#provideNetworkLayer(LayerParameters)} in order to get a
     * {@link TransportLayer} implementation.
     * Retrieved {@link TransportLayer} implementation will be wrapped with internal {@link NetworkLayer}
     * implementation.
     *
     * @return an implementation of transport TCP/IP layer for internet cards
     */
    protected abstract TransportLayer provideTransportLayer(LayerParameters layerParameters);

    @Override
    protected final NetworkLayer provideNetworkLayer(final LayerParameters layerParameters) {
        final LayerParameters transportParameters = InetUtils.nextLayerParameters(layerParameters, "Transport");
        final TransportLayer transportLayer = provideTransportLayer(transportParameters);
        return InetUtils.createLayerIfNotStub(transportLayer, DefaultNetworkLayer::new);
    }
}
