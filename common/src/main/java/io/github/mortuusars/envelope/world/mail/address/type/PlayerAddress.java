package io.github.mortuusars.envelope.world.mail.address.type;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class PlayerAddress implements Address {
    public static final MapCodec<PlayerAddress> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ExtraCodecs.PLAYER_NAME.fieldOf("name").forGetter(PlayerAddress::getString)
    ).apply(instance, PlayerAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerAddress> STREAM_CODEC =
          ByteBufCodecs.STRING_UTF8.map(PlayerAddress::new, PlayerAddress::getString).cast();

    private final String name;

    public PlayerAddress(String name) {
        Preconditions.checkArgument(!StringUtil.isBlank(name) && StringUtil.isValidPlayerName(name),
              name + " is not valid PlayerAddress. Too long or contains illegal characters.");
        this.name = name;
    }

    public PlayerAddress(Player player) {
        this(player.getScoreboardName());
    }

    @Override
    public Type getType() {
        return Type.PLAYER;
    }

    public String getString() {
        return name;
    }

    public MutableComponent getComponent() {
        return Component.literal(name);
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerAddress that = (PlayerAddress) o;
        return this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return ("p" + name.toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Player[" + name + "]";
    }
}
