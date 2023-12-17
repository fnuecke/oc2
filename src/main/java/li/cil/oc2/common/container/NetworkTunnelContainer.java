/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.NetworkTunnelItem;
import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public final class NetworkTunnelContainer extends AbstractContainer {
    public static void createServer(final ServerPlayer player, final InteractionHand hand) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return player.getItemInHand(hand).getItem().getDescription();
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                return new NetworkTunnelContainer(id, player, hand);
            }
        }, b -> b.writeEnum(hand));
    }

    public static NetworkTunnelContainer createClient(final int id, final Inventory inventory, final FriendlyByteBuf data) {
        final InteractionHand hand = data.readEnum(InteractionHand.class);
        final Player player = inventory.player;
        return new NetworkTunnelContainer(id, player, hand);
    }

    ///////////////////////////////////////////////////////////////////

    private final Player player;
    private final InteractionHand hand;
    private final Container linkSlot = new SimpleContainer(1);

    ///////////////////////////////////////////////////////////////////

    private NetworkTunnelContainer(final int id, final Player player, final InteractionHand hand) {
        super(Containers.NETWORK_TUNNEL.get(), id);
        this.player = player;
        this.hand = hand;

        createPlayerInventoryAndHotbarSlots(player.getInventory(), 8, 115);

        addSlot(new LockedSlot(player.getInventory(), getHandSlot(), 80, 25));
        addSlot(new DeviceTypeSlot(linkSlot, DeviceTypes.NETWORK_TUNNEL, 0, 80, 51));
    }

    ///////////////////////////////////////////////////////////////////

    public boolean hasLinkSlotItem() {
        return !linkSlot.getItem(0).isEmpty();
    }

    public void createTunnel() {
        final ItemStack tunnelA = player.getItemInHand(hand);
        final ItemStack tunnelB = linkSlot.getItem(0);

        if (tunnelA.isEmpty() ||
            tunnelB.isEmpty() ||
            !tunnelA.is(ItemTags.DEVICES_NETWORK_TUNNEL) ||
            !tunnelB.is(ItemTags.DEVICES_NETWORK_TUNNEL)) {
            return;
        }

        final UUID id = UUID.randomUUID();
        NetworkTunnelItem.setTunnelId(tunnelA, id);
        NetworkTunnelItem.setTunnelId(tunnelB, id);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);

        if (!player.getLevel().isClientSide()) {
            clearContainer(player, linkSlot);
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return player.getItemInHand(hand).is(ItemTags.DEVICES_NETWORK_TUNNEL);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected boolean isSlotLocked(final Inventory inventory, final int slot) {
        return inventory.getItem(slot) == player.getItemInHand(hand);
    }

    ///////////////////////////////////////////////////////////////////

    private int getHandSlot() {
        final Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == player.getItemInHand(hand)) {
                return slot;
            }
        }

        return -1;
    }
}
