package io.github.mortuusars.envelope.integration.jei.ingredient;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiPlugin;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityAddressIngredientHelper implements IIngredientHelper<EntityAddress> {
    @Override
    public @NotNull IIngredientType<EntityAddress> getIngredientType() {
        return EnvelopeJeiPlugin.ENTITY_ADDRESS_INGREDIENT;
    }

    @Override
    public @NotNull String getDisplayName(EntityAddress ingredient) {
        return "Address";
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull String getUniqueId(EntityAddress ingredient, UidContext context) {
        return ingredient.getString();
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(EntityAddress ingredient) {
        return ingredient.getEntityHolder()
              .unwrapKey()
              .map(ResourceKey::location)
              .orElse(Envelope.resource("unknown"));
    }

    @Override
    public @NotNull EntityAddress copyIngredient(EntityAddress ingredient) {
        return ingredient;
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable EntityAddress ingredient) {
        if (ingredient != null) {
            return ingredient.toString();
        }
        return "Entity Address";
    }
}
