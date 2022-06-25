package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.inet.InternetManagerImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public final class InternetCardDevice extends AbstractNetworkInterfaceDevice {

    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////

    private InternetConnection internetConnection = null;

    ///////////////////////////////////////////////////////////////

    public InternetCardDevice(final ItemStack identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////

    private void openInternetAccess() {
        LOGGER.debug("Connect internet card");
        closeInternetAccess();
        final InternetAdapter internetAdapter = new InternetAdapterImpl(getNetworkInterface());
        InternetManagerImpl.getInstance()
            .ifPresent(internetManager -> internetConnection = internetManager.connect(internetAdapter, internetAdapterState));
    }

    private void closeInternetAccess() {
        if (internetConnection != null) {
            LOGGER.debug("Disconnect internet card");
            internetConnection.stop();
            internetConnection = null;
        }
    }

    private Tag internetAdapterState = null;

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        super.deserializeNBT(tag);
        internetAdapterState = tag.get(Constants.INTERNET_ADAPTER_TAG_NAME);
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = super.serializeNBT();
        final InternetConnection internetConnection = this.internetConnection;
        if (internetConnection != null) {
            internetConnection.saveAdapterState()
                .ifPresent(adapterState -> {
                    tag.put(Constants.INTERNET_ADAPTER_TAG_NAME, adapterState);
                    // TODO: not sure, if this is meaningful
                    internetAdapterState = adapterState;
                });
        }
        return tag;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        final VMDeviceLoadResult result = super.mount(context);
        openInternetAccess();
        return result;
    }

    @Override
    public void unmount() {
        super.unmount();
        closeInternetAccess();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private record InternetAdapterImpl(NetworkInterface networkInterface) implements InternetAdapter {

        @Nullable
        @Override
        public byte[] receiveEthernetFrame() {
            return networkInterface.readEthernetFrame();
        }

        @Override
        public void sendEthernetFrame(final byte[] frame) {
            networkInterface.writeEthernetFrame(networkInterface, frame, 64);
        }
    }
}
