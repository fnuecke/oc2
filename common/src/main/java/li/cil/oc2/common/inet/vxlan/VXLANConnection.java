package li.cil.oc2.common.inet.vxlan;

import li.cil.oc2.common.inet.GatewayConnection;
import li.cil.oc2.common.inet.InternetAdapter;

public class VXLANConnection implements GatewayConnection  {

    VXLANConnection(InternetAdapter adapter, String peer, int port, int vni) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }

}
