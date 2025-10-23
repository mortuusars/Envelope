package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.core.address.AddressDisplay;
import io.github.mortuusars.envelope.core.address.AllAddresses;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenAddressTagScreenS2CP;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AddressTagItem extends Item {
    public AddressTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS);
        if (address != null) {
            tooltipComponents.add(AddressDisplay.createGeneric(address));
        }
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        if (!slot.getItem().is(Envelope.Tags.Items.MAILABLE)) return false;
        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS);
        if (address == null) return false;
        if (address.equals(slot.getItem().get(Envelope.DataComponents.MAIL_RECIPIENT))) return true; // Do not swap stacks, just do nothing

        slot.getItem().set(Envelope.DataComponents.MAIL_RECIPIENT, address);

        // Having old info with new recipient address would make no sense (and may confuse someone)
        slot.getItem().remove(Envelope.DataComponents.MAIL_SENDER);
        slot.getItem().remove(Envelope.DataComponents.MAIL_DELIVERY_LOG);

        stack.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        return true;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            AllAddresses knownAddresses = serverPlayer.serverLevel().getEnvelopeContext().getKnownAddresses();
            Packets.sendToClient(new OpenAddressTagScreenS2CP(usedHand, knownAddresses), serverPlayer);
            player.getCooldowns().addCooldown(this, 6);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }
}
