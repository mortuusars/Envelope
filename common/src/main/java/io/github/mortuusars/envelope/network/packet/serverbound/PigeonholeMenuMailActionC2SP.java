package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record PigeonholeMenuMailActionC2SP(int index, PigeonholeMenu.Action action) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("mailbox_menu_mail_action");
    public static final CustomPacketPayload.Type<PigeonholeMenuMailActionC2SP> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, PigeonholeMenuMailActionC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PigeonholeMenuMailActionC2SP::index,
            PigeonholeMenu.Action.STREAM_CODEC, PigeonholeMenuMailActionC2SP::action,
            PigeonholeMenuMailActionC2SP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (!(player.containerMenu instanceof PigeonholeMenu pigeonholeMenu)) {
            Envelope.LOGGER.error("Cannot handle '{}' packet: Player '{}' does not have PigeonholeMenu open.", ID, player);
            return false;
        }

        pigeonholeMenu.doMailAction(player, index, action);

        return true;
    }
}
