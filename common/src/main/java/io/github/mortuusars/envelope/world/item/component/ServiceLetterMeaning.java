package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record ServiceLetterMeaning(ResourceLocation id) {
    public static final Codec<ServiceLetterMeaning> CODEC =
          ResourceLocation.CODEC.xmap(ServiceLetterMeaning::new, ServiceLetterMeaning::id);
    public static final StreamCodec<ByteBuf, ServiceLetterMeaning> STREAM_CODEC =
          ResourceLocation.STREAM_CODEC.map(ServiceLetterMeaning::new, ServiceLetterMeaning::id);

    public MutableComponent translate() {
        return Component.translatable(id.toLanguageKey("service_letter_meaning").replace("/", "."));
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        ServiceLetterMeaning that = (ServiceLetterMeaning) object;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public @NotNull String toString() {
        return "ServiceLetterMeaning{" + id + '}';
    }
}
