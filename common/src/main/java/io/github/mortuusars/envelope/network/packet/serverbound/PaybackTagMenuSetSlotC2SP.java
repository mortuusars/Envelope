package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record PaybackTagMenuSetSlotC2SP(int slotIndex, ItemStack item) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("payback_tag_menu_set_slot");
    public static final Type<PaybackTagMenuSetSlotC2SP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackTagMenuSetSlotC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PaybackTagMenuSetSlotC2SP::slotIndex,
            ItemStack.STREAM_CODEC, PaybackTagMenuSetSlotC2SP::item,
            PaybackTagMenuSetSlotC2SP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (!(player.containerMenu instanceof PaybackTagMenu menu)) {
            Envelope.LOGGER.error("Cannot handle {} packet from {}: Player does not have PaybackTagMenu open.",
                  ID, player.getScoreboardName());
            return false;
        }

        if (slotIndex < 0 || slotIndex > PaybackRequest.SLOTS) {
            Envelope.LOGGER.error("Cannot handle {} packet from {}: slot {} is not in valid range (0 - {}).",
                    ID, player.getScoreboardName(), slotIndex, PaybackRequest.SLOTS);
            return false;
        }

        Slot slot = menu.slots.get(slotIndex);

        if (slot.container instanceof Inventory) { // Extra check to make sure it's not an inventory
            Envelope.LOGGER.error("Cannot handle {} packet from {}: slot {} is for player inventory O_O.",
                  ID, player.getScoreboardName(), slotIndex);
            return false;
        }

        slot.setByPlayer(item);
        return true;
    }
}
