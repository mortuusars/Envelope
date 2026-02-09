package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.MailboxAddressTagApplyC2SP;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class MailboxChangeAddressScreen extends AbstractMailboxAddressScreen {
    protected final BlockPos pos;
    protected final BlockState state;
    protected final BlockEntity blockEntity;

    public MailboxChangeAddressScreen(InteractionHand hand, AllAddresses.Realized knownAddresses,
                                      BlockPos pos, BlockAddress existingAddress,
                                      Component title) {
        super(hand, knownAddresses, Optional.ofNullable(existingAddress), title);
        this.pos = pos;
        this.state = player.level().getBlockState(pos);
        this.blockEntity = player.level().getBlockEntity(pos);
    }

    @Override
    protected ItemStack getTargetPreview() {
        return new ItemStack(state.getBlock().asItem());
    }

    @Override
    protected void onConfirm() {
        Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f));
        if (Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get() > 0) {
            player.level().playSound(player, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS);
        }
        Packets.sendToServer(new MailboxAddressTagApplyC2SP(hand, getCurrentAddressId().trim(), pos));
    }

    @Override
    protected boolean stillValid() {
        return player.getItemInHand(hand).getItem() instanceof AddressTagItem
              && Container.stillValidBlockEntity(blockEntity, player);
    }
}