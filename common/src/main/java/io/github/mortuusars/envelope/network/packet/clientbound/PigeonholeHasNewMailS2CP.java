package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class PigeonholeHasNewMailS2CP implements Packet {
    public static final PigeonholeHasNewMailS2CP INSTANCE = new PigeonholeHasNewMailS2CP();
    public static final ResourceLocation ID = Envelope.resource("pigeonhole_has_new_mail");
    public static final Type<PigeonholeHasNewMailS2CP> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PigeonholeHasNewMailS2CP> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    private PigeonholeHasNewMailS2CP() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player.containerMenu instanceof PigeonholeMenu pigeonholeMenu)) {
            Envelope.LOGGER.error("Cannot handle '{}' packet: Player '{}' does not have PigeonholeMenu open.", ID, player);
            return false;
        }

        pigeonholeMenu.setHasNewMail(true);
        player.level().playSound(player, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 0.75f, 1f);
        return true;
    }
}

