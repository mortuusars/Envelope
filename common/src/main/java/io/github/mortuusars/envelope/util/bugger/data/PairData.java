package io.github.mortuusars.envelope.util.bugger.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import net.minecraft.resources.ResourceLocation;

public class PairData<A, B> extends Data<Pair<A, B>> {
    public PairData(ResourceLocation id, MapCodec<A> firstCodec, MapCodec<B> secondCodec) {
        super(id, codec(firstCodec, secondCodec));
    }

    public PairData<A, B> send(A first, B second) {
        if (!Bugger.isEnabled()) return this;
        send(new Pair<>(first, second));
        return this;
    }

    @Override
    public void handle(Pair<A, B> value) {
        handle(value.getFirst(), value.getSecond());
    }

    public void handle(A first, B second) {
    }

    public static <A, B> Codec<Pair<A, B>> codec(MapCodec<A> firstCodec, MapCodec<B> secondCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
              firstCodec.forGetter(Pair::getFirst),
              secondCodec.forGetter(Pair::getSecond)
        ).apply(instance, Pair::new));
    }
}
