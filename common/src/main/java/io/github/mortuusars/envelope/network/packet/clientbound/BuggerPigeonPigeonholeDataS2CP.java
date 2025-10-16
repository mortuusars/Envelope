package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record BuggerPigeonPigeonholeDataS2CP(int id,
                                             Optional<BlockPos> pigeonholePos,
                                             int cooldownBeforeEnteringPigeonhole,
                                             int cooldownBeforeWantingToEnterPigeonhole,
                                             int cooldownBeforeLocatingNewPigeonhole) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("bugger_pigeon_pigeonhole_data");
    public static final Type<BuggerPigeonPigeonholeDataS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BuggerPigeonPigeonholeDataS2CP> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, BuggerPigeonPigeonholeDataS2CP::id,
          ByteBufCodecs.optional(BlockPos.STREAM_CODEC), BuggerPigeonPigeonholeDataS2CP::pigeonholePos,
          ByteBufCodecs.VAR_INT, BuggerPigeonPigeonholeDataS2CP::cooldownBeforeEnteringPigeonhole,
          ByteBufCodecs.VAR_INT, BuggerPigeonPigeonholeDataS2CP::cooldownBeforeWantingToEnterPigeonhole,
          ByteBufCodecs.VAR_INT, BuggerPigeonPigeonholeDataS2CP::cooldownBeforeLocatingNewPigeonhole,
          BuggerPigeonPigeonholeDataS2CP::new
    );

    public BuggerPigeonPigeonholeDataS2CP(int id, PigeonholeHandler handler) {
        this(id, Optional.ofNullable(handler.getPigeonholePos()), handler.getCooldownBeforeEnteringPigeonhole(),
              handler.getCooldownBeforeWantingToEnterPigeonhole(), handler.getCooldownBeforeLocatingNewPigeonhole());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (player.level().getEntity(id) instanceof Pigeon pigeon) {
            pigeon.getPigeonholeHandler().setPigeonholePos(pigeonholePos.orElse(null));
            pigeon.getPigeonholeHandler().setCooldownBeforeEnteringPigeonhole(cooldownBeforeEnteringPigeonhole);
            pigeon.getPigeonholeHandler().setCooldownBeforeWantingToEnterPigeonhole(cooldownBeforeWantingToEnterPigeonhole);
            pigeon.getPigeonholeHandler().setCooldownBeforeLocatingNewPigeonhole(cooldownBeforeLocatingNewPigeonhole);
        }
        return true;
    }
}
