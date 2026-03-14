package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class CustomMailingRecipe implements MailingRecipe {
    private final EntityAddress address;

    public CustomMailingRecipe(EntityAddress address) {
        this.address = address;
    }

    @Override
    public EntityAddress getAddress() {
        return address;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public CraftingResult craft(PackageContents input, Address sender, ServerLevel level) {
        return new CraftingResult(
              consumeInput(input),
              createOutput(input, sender, level),
              0);
    }

    @Override
    public abstract boolean matches(PackageContents input, Level level);
    @Override
    public abstract @NotNull ItemStack assemble(PackageContents input, HolderLookup.Provider registries);

//    public static class Serializer implements RecipeSerializer<CompassAddressingRecipe> {
//        public static final MapCodec<CompassAddressingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
//              RegistryFixedCodec.create(Envelope.Registries.MAIL_ENTITY)
//                    .xmap(EntityAddress::new, EntityAddress::getEntityHolder)
//                    .fieldOf("entity")
//                    .forGetter(CompassAddressingRecipe::getAddress)
//        ).apply(i, CompassAddressingRecipe::new));
//
//        public static final StreamCodec<RegistryFriendlyByteBuf, CompassAddressingRecipe> STREAM_CODEC = StreamCodec.composite(
//              EntityAddress.STREAM_CODEC, CompassAddressingRecipe::getAddress,
//              CompassAddressingRecipe::new
//        );
//
//        @Override
//        public @NotNull MapCodec<CompassAddressingRecipe> codec() {
//            return CODEC;
//        }
//
//        @Override
//        public @NotNull StreamCodec<RegistryFriendlyByteBuf, CompassAddressingRecipe> streamCodec() {
//            return STREAM_CODEC;
//        }
//    }
}
