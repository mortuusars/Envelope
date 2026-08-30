package io.github.mortuusars.envelope.network.handler;

import io.github.mortuusars.envelope.client.gui.screen.*;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.network.packet.clientbound.*;
import io.github.mortuusars.envelope.world.item.LetterAndQuillItem;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.MailboxBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ClientboundPacketsHandler {
    public static void openLetterEditScreen(ClientboundOpenLetterEditScreenPacket packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterAndQuillItem) {
            Minecrft.get().setScreen(new LetterEditScreen(itemInHand, packet.hand()));
        }
    }

    public static void openLetterViewScreen(ClientboundOpenLetterViewScreenPacket packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterItem) {
            Minecrft.get().setScreen(new LetterViewScreen(itemInHand, packet.hand()));
        }
    }

    public static void openPlacedLetterViewScreen(ClientboundOpenLetterBlockViewScreenPacket packet) {
        Minecrft.get().setScreen(new LetterBlockViewScreen(packet.letter(), packet.pos()));
    }

    public static void openAddressTagScreen(ClientboundOpenAddressTagScreenPacket packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof AddressTagItem) {
            AddressTagScreen screen = new AddressTagScreen(packet.hand(), packet.knownAddresses(),
                  Component.translatable("gui.envelope.address_tag.title"));
            Minecrft.get().setScreen(screen);
        }
    }

    public static void openMailboxPlacingScreen(ClientboundOpenMailboxPlacingScreenPacket packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof MailboxBlockItem) {
            MailboxPlacingScreen screen = new MailboxPlacingScreen(packet.hand(), packet.knownAddresses(),
                  packet.hitResult(), Component.translatable("gui.envelope.mailbox_choose_address.title"));
            Minecrft.get().setScreen(screen);
        }
    }

    public static void openMailboxAddressTagScreen(ClientboundOpenMailboxAddressTagScreenPacket packet) {
        if (Minecrft.player().getItemInHand(packet.hand()).getItem() instanceof AddressTagItem) {
            MailboxChangeAddressScreen screen = new MailboxChangeAddressScreen(packet.hand(),
                  packet.knownAddresses(), packet.pos(), packet.currentAddress(),
                  Component.translatable("gui.envelope.mailbox_change_address.title"));
            Minecrft.get().setScreen(screen);
        }
    }
}