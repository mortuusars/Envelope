package io.github.mortuusars.envelope.network.handler;

import io.github.mortuusars.envelope.client.gui.screen.AddressTagScreen;
import io.github.mortuusars.envelope.client.gui.screen.LetterEditScreen;
import io.github.mortuusars.envelope.client.gui.screen.PigeonholeAddressTagScreen;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenAddressTagScreenS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenLetterEditScreenS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenPigeonholeAddressTagScreenS2CP;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ClientPacketsHandler {
    public static void openLetterEditScreen(OpenLetterEditScreenS2CP packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterItem) {
            Minecrft.get().setScreen(new LetterEditScreen(itemInHand, packet.hand()));
        }
    }

    public static void openAddressTagScreen(OpenAddressTagScreenS2CP packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof AddressTagItem) {
            Minecrft.get().setScreen(new AddressTagScreen(packet.hand(), packet.knownAddresses(), Component.translatable("gui.envelope.address_tag.title")));
        }
    }

    public static void openPigeonholeAddressTagScreen(OpenPigeonholeAddressTagScreenS2CP packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof AddressTagItem) {
            Minecrft.get().setScreen(new PigeonholeAddressTagScreen(packet.hand(),
                    packet.knownAddresses(), packet.pos(), packet.currentAddress(), Component.translatable("gui.envelope.pigeonhole_address_tag.title")));
        }
    }
}