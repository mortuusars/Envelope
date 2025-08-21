package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PigeonholeMenuMailS2CP(List<ItemStack> mail) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("pigeonhole_menu_mail");
    public static final Type<PigeonholeMenuMailS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonholeMenuMailS2CP> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), PigeonholeMenuMailS2CP::mail,
            PigeonholeMenuMailS2CP::new
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

        pigeonholeMenu.setMail(mail);

        return true;
    }
}
