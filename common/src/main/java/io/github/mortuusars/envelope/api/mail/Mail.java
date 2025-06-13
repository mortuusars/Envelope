package io.github.mortuusars.envelope.api.mail;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Mail(Sender sender, String recipient, ItemStack content, long sentAt, int travelDuration, Status status) {
    public Mail {
        Preconditions.checkArgument(!recipient.isBlank(), "Recipient cannot be empty!");
        Preconditions.checkArgument(!content.isEmpty(), "Content cannot be empty!");
    }

    // --

    public record Sender(@Nullable UUID uuid, String name, Type type) {
        public enum Type {
            PLAYER,
            NPC;
        }

        public static Sender of(Player player) {
            return new Sender(player.getUUID(), player.getScoreboardName(), Type.PLAYER);
        }
    }

    public enum Status {
        REGULAR,
        RETURNED,
        REJECTED;
    }
}
