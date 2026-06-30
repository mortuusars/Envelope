package io.github.mortuusars.envelope.advancements.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.advancements.critereon.CollectionPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ItemPackagePredicate(Optional<CollectionPredicate<ItemStack, ItemPredicate>> items) implements SingleComponentItemPredicate<PackageContents> {
    public static final Codec<ItemPackagePredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
                CollectionPredicate.codec(ItemPredicate.CODEC).optionalFieldOf("items").forGetter(ItemPackagePredicate::items))
          .apply(i, ItemPackagePredicate::new)
    );

    @Override
    public @NotNull DataComponentType<PackageContents> componentType() {
        return Envelope.DataComponents.PACKAGE_CONTENTS;
    }

    public boolean matches(ItemStack stack, PackageContents contents) {
        return this.items.isEmpty() || this.items.get().test(contents.getItems());
    }
}
