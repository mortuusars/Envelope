package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public abstract class MailRecipeBuilder {
    private final ServiceAddress address;

    public MailRecipeBuilder(ServiceAddress address) {
        this.address = address;
    }

    public ServiceAddress getAddress() {
        return address;
    }

    public abstract ItemStack getResult();

    public abstract void save(RecipeOutput output, ResourceLocation id);

    public void save(RecipeOutput output, String name) {
        save(output, Envelope.resource(getDefaultPath(getAddress(), name)));
    }

    public void save(RecipeOutput output) {
        save(output, getDefaultRecipeName(getResult().getItem()));
    }

    // --

    public static MailCraftingRecipeBuilder crafting(ServiceAddress address) {
        return new MailCraftingRecipeBuilder(address);
    }

    protected String getDefaultRecipeName(ItemLike itemLike) {
        return BuiltInRegistries.ITEM.getKey(itemLike.asItem()).getPath();
    }

    public static String getDefaultPath(ServiceAddress address, String name) {
        String addressStr = address.getDefinitionHolder().unwrapKey()
              .map(key -> key.location().getPath())
              .orElseThrow();
        return "mailing/" + addressStr + "/" + name;
    }
}
