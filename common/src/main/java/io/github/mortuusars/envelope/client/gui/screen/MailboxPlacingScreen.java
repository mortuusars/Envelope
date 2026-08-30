package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.network.packet.serverbound.ServerboundMailboxPlacePacket;
import io.github.mortuusars.envelope.world.item.MailboxBlockItem;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

public class MailboxPlacingScreen extends AbstractMailboxAddressScreen {
    protected final LocalPlayer player;
    protected final BlockHitResult hitResult;

    public MailboxPlacingScreen(InteractionHand hand, AllAddresses knownAddresses, BlockHitResult hitResult, Component title) {
        super(hand, knownAddresses, Optional.empty(), title);
        this.player = Minecrft.player();
        this.hitResult = hitResult;
        this.existingAddress = Optional.empty();
    }

    @Override
    protected ItemStack getTargetPreview() {
        //TODO: Support for proper block state preview. As it will look when placed.
        return Minecrft.player().getItemInHand(hand);
    }

    @Override
    protected void onConfirm() {
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        if (blockItem.place(new BlockPlaceContext(player, hand, stack, hitResult)).consumesAction()) {
            if (Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get() > 0) {
                player.level().playSound(player, hitResult.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS);
            }

            new ServerboundMailboxPlacePacket(hand, getCurrentAddressId().trim(), hitResult).sendToServer();
        }
    }

    @Override
    protected boolean stillValid() {
        return player.getItemInHand(hand).getItem() instanceof MailboxBlockItem;
    }

    @Override
    protected boolean isCurrentIdSameAsExistingAddress() {
        return false;
    }
}
