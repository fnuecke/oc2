package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LinkLocalLayer;
import li.cil.oc2.api.inet.NetworkLayer;
import li.cil.oc2.api.inet.SessionLayer;
import li.cil.oc2.api.inet.TransportLayer;

public final class NullLayer implements LinkLocalLayer, NetworkLayer, TransportLayer, SessionLayer {
    public static final NullLayer INSTANCE = new NullLayer();

    private NullLayer() {
    }
}
