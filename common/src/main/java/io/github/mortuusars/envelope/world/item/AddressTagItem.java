package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.packet.clientbound.ClientboundOpenAddressTagScreenPacket;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.mortaar.world.item.ApplicatorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS_TAG_ADDRESS);
        if (address != null) {
            tooltipComponents.add(address.format()
                  .withIcon()
                  .withIconColor(Colors.ADDRESS_NEUTRAL)
                  .withColor(Colors.ADDRESS_NEUTRAL)
                  .toComponent());
        }
    }

    @Override
    public boolean shouldRenderSlotTooltipWhileCarrying(Player player, AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        if (!slot.allowModification(player)) {
            return false;
        }

        return slot.getItem().is(Envelope.Tags.Items.MAILABLE)
            && (carried.has(Envelope.DataComponents.ADDRESS_TAG_ADDRESS) || slot.getItem().has(Envelope.DataComponents.MAIL_ADDRESS_TAG));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();

        if (action != ClickAction.SECONDARY
              || !slot.allowModification(player)
              || !target.is(Envelope.Tags.Items.MAILABLE)) {
            return false;
        }

        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS_TAG_ADDRESS);
        @Nullable Address recipient = Mail.getRecipient(target).orElse(null);
        if (Objects.equals(address, recipient)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK, 1, 1);
            return true;
        }

        if (address != null && stack.getCount() < target.getCount()) {
            player.playSound(SoundEvents.COMPARATOR_CLICK, 1, 1);
            return true;
        }

        ItemStack result = target.copy();
        Mail.setRecipient(result, address);
        Mail.removePreviousDeliveryData(result);

        if (!slot.mayPlace(result)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK, 1, 1);
            return true;
        }

        slot.setByPlayer(result);

        if (address != null && !player.isCreative()) {
            stack.shrink(target.getCount());
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
            new ClientboundOpenAddressTagScreenPacket(usedHand, knownAddresses).sendToClient(serverPlayer);
            player.getCooldowns().addCooldown(this, 6);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }
}
