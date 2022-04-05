package li.cil.oc2.api.inet.provider;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.layer.LinkLocalLayer;

/**
 * Internet access provider for oc2:internet-card item.
 * At initialization phase one implementation of this interface will be loaded via {@link java.util.ServiceLoader}.
 * If no implementation is found, then the default one will be used instead.
 * <p>
 * It is recommended to not implement this interface directly.
 * There are several abstract classes for several levels of TCP/IP stack:
 *
 * <ul>
 *     <li>{@link LinkLocalLayerInternetProvider}</li>
 *     <li>{@link NetworkLayerInternetProvider}</li>
 *     <li>{@link TransportLayerInternetProvider}</li>
 *     <li>{@link SessionLayerInternetProvider}</li>
 * </ul>
 * <p>
 * Each of these classes implements {@link InternetProvider} and
 * asks you to provide an implementation of corresponding TCP/IP layer.
 *
 * @see LinkLocalLayerInternetProvider
 * @see NetworkLayerInternetProvider
 * @see TransportLayerInternetProvider
 * @see SessionLayerInternetProvider
 */
public interface InternetProvider {

    /**
     * This method should provide and implementation of {@link LinkLocalLayer} interface and not fail.
     * It will be called once for each loaded internet card.
     *
     * @return an implementation of {@link LinkLocalLayer} interface
     */
    LinkLocalLayer provideInternet(LayerParameters layerParameters);
}
