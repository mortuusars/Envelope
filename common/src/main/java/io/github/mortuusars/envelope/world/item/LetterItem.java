package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.api.mail.Recipient;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenLetterEditScreenS2CP;
import io.github.mortuusars.envelope.world.KnownPlayers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LetterItem extends Item {
    public LetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Map<String, UUID> knownPlayers = KnownPlayers.get(serverPlayer.serverLevel().getServer()).getAll();
            List<Recipient> recipients = knownPlayers.entrySet().stream()
                    .map(e -> new Recipient(e.getKey(), Optional.ofNullable(e.getValue()), Recipient.Type.PLAYER))
                    .toList();
            Packets.sendToClient(new OpenLetterEditScreenS2CP(usedHand, recipients), serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }
}
