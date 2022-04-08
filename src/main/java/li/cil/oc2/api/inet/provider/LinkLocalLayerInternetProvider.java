package li.cil.oc2.api.inet.provider;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.common.inet.*;

/**
 * An {@link InternetProvider} partial implementation that expects an {@link LinkLocalLayer} implementation from
 * protected method {@link LinkLocalLayerInternetProvider#provideLinkLocalLayer(LayerParameters)}.
 *
 * @see InternetProvider
 * @see LinkLocalLayer
 */
public abstract class LinkLocalLayerInternetProvider implements InternetProvider {

    protected static LinkLocalLayer nullLinkLocalLayer() {
        return NullLayer.INSTANCE;
    }

    protected static LinkLocalLayer defaultLinkLocalLayer(final LayerParameters layerParameters) {
        final LayerParameters networkParameters = InetUtils.nextLayerParameters(layerParameters, NetworkLayer.LAYER_NAME);
        final NetworkLayer networkLayer = NetworkLayerInternetProvider.defaultNetworkLayer(networkParameters);
        return new DefaultLinkLocalLayer(layerParameters, networkLayer);
    }

    /**
     * This method is called from {@link LinkLocalLayerInternetProvider#provideInternet(LayerParameters)} in order to get an
     * {@link LinkLocalLayer} implementation.
     *
     * @return an implementation of link local TCP/IP layer for internet cards
     */
    protected abstract LinkLocalLayer provideLinkLocalLayer(LayerParameters layerParameters);

    @Override
    public final LinkLocalLayer provideInternet(final LayerParameters layerParameters) {
        return provideLinkLocalLayer(InetUtils.nextLayerParameters(layerParameters, LinkLocalLayer.LAYER_NAME));
    }
}
