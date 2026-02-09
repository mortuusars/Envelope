package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressValidation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class PlayerAddress implements Address.Realized {
    public static final MapCodec<PlayerAddress> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Address.ID_CODEC.fieldOf("id").forGetter(PlayerAddress::getId)
    ).apply(instance, PlayerAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerAddress> STREAM_CODEC =
          ByteBufCodecs.STRING_UTF8.map(PlayerAddress::new, PlayerAddress::getId).cast();

    private final String id;

    public PlayerAddress(String getId) {
        AddressValidation.validateId(getId).getOrThrow();
        this.id = getId;
    }

    public PlayerAddress(Player player) {
        this(player.getScoreboardName());
    }

    @Override
    public Type getType() {
        return Type.PLAYER;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getDisplayString() {
        return id;
    }

    public MutableComponent getDisplayComponent() {
        return Component.literal(id);
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerAddress that = (PlayerAddress) o;
        return this.id.equalsIgnoreCase(that.id);
    }

    @Override
    public int hashCode() {
        return ("p" + id.toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Player[" + id + "]";
    }
}
