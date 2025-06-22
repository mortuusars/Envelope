package io.github.mortuusars.envelope.network.handler;

import io.github.mortuusars.envelope.client.gui.screen.LetterEditScreen;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenLetterEditScreenS2CP;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.world.item.ItemStack;

public class ClientPacketsHandler {
    public static void openLetterEditScreen(OpenLetterEditScreenS2CP packet) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterItem) {
            Minecrft.get().setScreen(new LetterEditScreen(itemInHand, packet.hand(), packet.knownRecipients()));
        }
    }
}