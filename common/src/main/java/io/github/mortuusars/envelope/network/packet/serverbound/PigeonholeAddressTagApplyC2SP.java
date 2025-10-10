package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public record PigeonholeAddressTagApplyC2SP(int slot, String addressId, BlockPos pos) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("pigeonhole_address_tag_apply");
    public static final Type<PigeonholeAddressTagApplyC2SP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonholeAddressTagApplyC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PigeonholeAddressTagApplyC2SP::slot,
            ByteBufCodecs.STRING_UTF8, PigeonholeAddressTagApplyC2SP::addressId,
            BlockPos.STREAM_CODEC, PigeonholeAddressTagApplyC2SP::pos,
            PigeonholeAddressTagApplyC2SP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (slot < 0 || slot > player.getInventory().items.size()) {
            Envelope.LOGGER.error("Cannot handle {} packet: slot {} is not in valid range (0 - {}).",
                    ID, slot, player.getInventory().items.size());
            return false;
        }

        ItemStack tag = player.getInventory().getItem(slot);

        if (!(tag.getItem() instanceof AddressTagItem)) {
            Envelope.LOGGER.error("Cannot handle {} packet: item in slot {} is not an AddressTagItem.", ID, slot);
            return false;
        }

        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof PigeonholeBlock block)) {
            Envelope.LOGGER.error("Cannot handle {} packet: block at pos [{}] is not a PigeonholeBlock", ID, pos);
            return false;
        }

        block.applyAddress(player, state, pos, slot, addressId);
        return true;
    }
}
