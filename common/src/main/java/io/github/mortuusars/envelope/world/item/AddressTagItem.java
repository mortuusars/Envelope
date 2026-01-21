package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenAddressTagScreenS2CP;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AddressTagItem extends Item implements ApplicatorItem {
    public AddressTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS);
        if (address != null) {
            tooltipComponents.add(address.format()
                  .withIcon()
                  .withIconColor(AddressFormatter.NEUTRAL_COLOR)
                  .withColor(AddressFormatter.NEUTRAL_COLOR)
                  .toComponent());
        }
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();

        if (action != ClickAction.SECONDARY
              || !slot.allowModification(player)
              || !target.is(Envelope.Tags.Items.MAILABLE)) {
            return false;
        }

        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS);
        @Nullable Address recipient = Mail.getRecipient(target).orElse(null);
        if (Objects.equals(address, recipient)) {
            return true; // Do nothing
        }

        ItemStack result = target.copy();
        Mail.setRecipient(result, address);
        Mail.removePreviousDeliveryData(result);

        if (!slot.mayPlace(result)) {
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 1);
            return true;
        }

        slot.setByPlayer(result);

        if (address != null) {
            stack.shrink(1);
        }

        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);

        return true;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof MailboxBlock) {
            return InteractionResult.FAIL; // Let mailbox handle the interaction
        }

        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            AllAddresses knownAddresses = MailService.of(serverPlayer.serverLevel()).getKnownAddresses();
            Packets.sendToClient(new OpenAddressTagScreenS2CP(usedHand, knownAddresses), serverPlayer);
            player.getCooldowns().addCooldown(this, 6);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }

    @Override
    public boolean shouldRenderTooltipWhileCarrying(Level level, ItemStack carried, ItemStack hovered) {
        return true;
    }
}
