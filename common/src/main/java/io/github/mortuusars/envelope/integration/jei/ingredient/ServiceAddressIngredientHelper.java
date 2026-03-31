package io.github.mortuusars.envelope.integration.jei.ingredient;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiPlugin;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServiceAddressIngredientHelper implements IIngredientHelper<ServiceAddress> {
    @Override
    public @NotNull IIngredientType<ServiceAddress> getIngredientType() {
        return EnvelopeJeiPlugin.SERVICE_ADDRESS_INGREDIENT;
    }

    @Override
    public @NotNull String getDisplayName(ServiceAddress ingredient) {
        return "Address";
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull String getUniqueId(ServiceAddress ingredient, UidContext context) {
        return ingredient.getString();
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(ServiceAddress ingredient) {
        return ingredient.getDefinitionHolder()
              .unwrapKey()
              .map(ResourceKey::location)
              .orElse(Envelope.resource("unknown"));
    }

    @Override
    public @NotNull ServiceAddress copyIngredient(ServiceAddress ingredient) {
        return ingredient;
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable ServiceAddress ingredient) {
        if (ingredient != null) {
            return ingredient.toString();
        }
        return "Service Address";
    }
}
