package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.layer.LinkLocalLayer;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.layer.TransportLayer;

public final class NullLayer implements LinkLocalLayer, NetworkLayer, TransportLayer, SessionLayer {
    public static final NullLayer INSTANCE = new NullLayer();

    private NullLayer() {
    }
}
