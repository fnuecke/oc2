package li.cil.oc2.api.inet;

import li.cil.oc2.common.inet.DefaultLinkLocalLayer;
import li.cil.oc2.common.inet.DefaultNetworkLayer;
import li.cil.oc2.common.inet.InetUtils;
import li.cil.oc2.common.inet.NullLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * An {@link InternetProvider} partial implementation that expects an {@link NetworkLayer} implementation from
 * protected method {@link NetworkLayerInternetProvider#provideNetworkLayer(LayerParameters)}.
 *
 * @see InternetProvider
 * @see NetworkLayer
 */
public abstract class NetworkLayerInternetProvider extends LinkLocalLayerInternetProvider {

    protected static NetworkLayer nullNetworkLayer() {
        return NullLayer.INSTANCE;
    }

    protected static NetworkLayer defaultNetworkLayer(final LayerParameters layerParameters) {
        final LayerParameters transportParameters = InetUtils.nextLayerParameters(layerParameters, TransportLayer.LAYER_NAME);
        final TransportLayer transportLayer = TransportLayerInternetProvider.defaultTransportLayer(transportParameters);
        return new DefaultNetworkLayer(layerParameters, transportLayer);
    }

    /**
     * This method is called from {@link NetworkLayerInternetProvider#provideLinkLocalLayer(LayerParameters)} in order to get a
     * {@link NetworkLayer} implementation.
     * Retrieved {@link NetworkLayer} implementation will be wrapped with internal {@link LinkLocalLayer}
     * implementation.
     *
     * @return an implementation of network TCP/IP layer for internet cards
     */
    protected abstract NetworkLayer provideNetworkLayer(LayerParameters layerParameters);

    @Override
    protected final LinkLocalLayer provideLinkLocalLayer(final LayerParameters layerParameters) {
        final LayerParameters networkParameters = InetUtils.nextLayerParameters(layerParameters, NetworkLayer.LAYER_NAME);
        final NetworkLayer networkLayer = provideNetworkLayer(networkParameters);
        return InetUtils.createLayerIfNotStub(networkLayer, layer -> new DefaultLinkLocalLayer(layerParameters, networkLayer));
    }
}
