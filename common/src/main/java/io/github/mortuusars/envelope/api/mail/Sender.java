package io.github.mortuusars.envelope.api.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Sender(String name, @Nullable UUID uuid, Type type) {
    public static final Codec<Sender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("name").forGetter(Sender::name),
                    UUIDUtil.CODEC.optionalFieldOf("uuid", null).forGetter(Sender::uuid),
                    Type.CODEC.fieldOf("type").forGetter(Sender::type))
            .apply(instance, Sender::new));

    public static Sender player(Player player) {
        return new Sender(player.getScoreboardName(), player.getUUID(), Type.PLAYER);
    }

    public static Sender npc(String name) {
        return new Sender(name, null, Type.NPC);
    }

    public Recipient toRecipient() {
        return new Recipient(name, uuid, type == Type.PLAYER ? Recipient.Type.PLAYER : Recipient.Type.NPC);
    }

    // --

    public enum Type implements StringRepresentable {
        PLAYER("player"),
        NPC("npc");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;

        Type(String npc) {
            this.name = npc;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
