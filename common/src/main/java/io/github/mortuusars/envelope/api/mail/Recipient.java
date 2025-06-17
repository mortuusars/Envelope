package io.github.mortuusars.envelope.api.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Recipient(String name, @Nullable UUID uuid, Type type) {
    public static final Codec<Recipient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("name").forGetter(Recipient::name),
                    UUIDUtil.CODEC.optionalFieldOf("uuid", null).forGetter(Recipient::uuid),
                    Type.CODEC.fieldOf("type").forGetter(Recipient::type))
            .apply(instance, Recipient::new));

    public static Recipient player(Player player) {
        return new Recipient(player.getScoreboardName(), player.getUUID(), Type.PLAYER);
    }

    public static Recipient npc(String name) {
        return new Recipient(name, null, Type.NPC);
    }

    public enum Type implements StringRepresentable {
        PLAYER("player"),
        NPC("npc"),
        UNKNOWN("unknown");

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
