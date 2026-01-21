package io.github.mortuusars.envelope.network.handler;

import io.github.mortuusars.envelope.client.gui.screen.*;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.packet.clientbound.*;
import io.github.mortuusars.envelope.world.item.LetterAndQuillItem;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.MailboxBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ClientPacketsHandler {
    public static void openLetterEditScreen(OpenLetterEditScreenS2CP packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterAndQuillItem) {
            Minecrft.get().setScreen(new LetterEditScreen(itemInHand, packet.hand()));
        }
    }

    public static void openLetterViewScreen(OpenLetterViewScreenS2CP packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterItem) {
            Minecrft.get().setScreen(new LetterViewScreen(itemInHand, packet.hand()));
        }
    }

    public static void openAddressTagScreen(OpenAddressTagScreenS2CP packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof AddressTagItem) {
            Minecrft.get().setScreen(new AddressTagScreen(packet.hand(), packet.knownAddresses(), Component.translatable("gui.envelope.address_tag.title")));
        }
    }

    public static void openMailboxPlacingScreen(OpenMailboxPlacingScreenS2CP packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof MailboxBlockItem) {
            Minecrft.get().setScreen(new MailboxPlacingScreen(packet.hand(), packet.knownAddresses(),
                  packet.hitResult(), Component.translatable("gui.envelope.mailbox_address_tag.title")));
        }
    }

    public static void openMailboxAddressTagScreen(OpenMailboxAddressTagScreenS2CP packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof AddressTagItem) {
            Minecrft.get().setScreen(new MailboxAddressTagScreen(packet.hand(),
                    packet.knownAddresses(), packet.pos(), packet.currentAddress(),
                  Component.translatable("gui.envelope.mailbox_address_tag.title")));
        }
    }
}