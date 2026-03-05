package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

public abstract class MailRecipeBuilder {
    private final EntityAddress address;

    public MailRecipeBuilder(EntityAddress address) {
        this.address = address;
    }

    public EntityAddress getAddress() {
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

    public static MailCraftingRecipeBuilder crafting(EntityAddress address) {
        return new MailCraftingRecipeBuilder(address);
    }

    protected String getDefaultRecipeName(ItemLike itemLike) {
        return BuiltInRegistries.ITEM.getKey(itemLike.asItem()).getPath();
    }

    public static String getDefaultPath(EntityAddress address, String name) {
        String addressStr = address.getEntityHolder().unwrapKey()
              .map(key -> key.location().getPath())
              .orElseThrow();
        return "mailing/" + addressStr + "/" + name;
    }
}
