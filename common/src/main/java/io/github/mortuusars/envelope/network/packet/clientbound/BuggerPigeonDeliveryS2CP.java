package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.entity.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record BuggerPigeonDeliveryS2CP(int id, Optional<Delivery> delivery) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("bugger_pigeon_delivery");
    public static final Type<BuggerPigeonDeliveryS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BuggerPigeonDeliveryS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BuggerPigeonDeliveryS2CP::id,
            ByteBufCodecs.optional(Delivery.STREAM_CODEC), BuggerPigeonDeliveryS2CP::delivery,
            BuggerPigeonDeliveryS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (player.level().getEntity(id) instanceof Pigeon pigeon) {
            pigeon.setDelivery(delivery.orElse(null));
        }
        return true;
    }
}
