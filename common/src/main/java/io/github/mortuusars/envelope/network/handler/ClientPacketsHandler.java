package io.github.mortuusars.envelope.network.handler;

import io.github.mortuusars.envelope.client.gui.screen.AddressTagScreen;
import io.github.mortuusars.envelope.client.gui.screen.LetterEditScreen;
import io.github.mortuusars.envelope.client.gui.screen.PigeonholeScreen;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenAddressTagScreenS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenLetterEditScreenS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeSyncBlockDataS2CP;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.world.item.ItemStack;

public class ClientPacketsHandler {
    public static void openLetterEditScreen(OpenLetterEditScreenS2CP packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterItem) {
            Minecrft.get().setScreen(new LetterEditScreen(itemInHand, packet.hand(), packet.knownRecipients()));
        }
    }

    public static void openAddressTagScreen(OpenAddressTagScreenS2CP packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof AddressTagItem) {
            Minecrft.get().setScreen(new AddressTagScreen(itemInHand, packet.hand(), packet.knownAddresses()));
        }
    }

    public static void syncPigeonholeBlockData(PigeonholeSyncBlockDataS2CP packet) {
        if (Minecrft.get().screen instanceof PigeonholeScreen screen) {
            screen.setOccupantsData(packet.occupants());
        }
    }
}