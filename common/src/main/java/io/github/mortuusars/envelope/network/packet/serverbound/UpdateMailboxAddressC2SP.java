package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PigeonholeAddressMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record UpdateMailboxAddressC2SP(String address) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("update_mailbox_address");
    public static final Type<UpdateMailboxAddressC2SP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateMailboxAddressC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), UpdateMailboxAddressC2SP::address,
            UpdateMailboxAddressC2SP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (player instanceof ServerPlayer serverPlayer && player.containerMenu instanceof PigeonholeAddressMenu pigeonholeAddressMenu) {
            pigeonholeAddressMenu.setAddressAndUpdateConfirmState(serverPlayer.serverLevel(), address);
        }
        return true;
    }
}
