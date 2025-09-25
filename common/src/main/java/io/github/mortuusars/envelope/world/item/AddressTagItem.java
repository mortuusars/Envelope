package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenAddressTagScreenS2CP;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class AddressTagItem extends Item {
    public AddressTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable Address address = stack.get(Envelope.DataComponents.ADDRESS);
        if (address != null) {
            tooltipComponents.add(Component.literal("✉ ").withStyle(ChatFormatting.GRAY)
                    .append(address.getDisplayName().withStyle(ChatFormatting.WHITE)));
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
        stack.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = serverPlayer.serverLevel();

            List<Address.Player> players = serverLevel.getEnvelopePlayerInformation().getKnownPlayers().getAllAddresses();
            Set<Address.Pigeonhole> pigeonholes = serverLevel.getEnvelopePigeonholeManager().getAllAddresses();
            // Set<Address.Npc> npcs = serverLevel.getEnvelopeNpcManager().getAllAddresses();

            List<Address> knownAddresses = new ArrayList<>(Stream.concat(players.stream(), pigeonholes.stream()).toList());

            Packets.sendToClient(new OpenAddressTagScreenS2CP(usedHand, knownAddresses), serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }
}
