package io.github.mortuusars.envelope.integration.jei.util;

import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiRecipeTypes;
import io.github.mortuusars.envelope.network.packet.serverbound.ServerboundPackingMenuPresetAddressPacket;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

public class PackingRecipeTransferInfo extends InHandRecipeTransferInfo<PackingMenu, RecipeHolder<MailRecipe>> {
    public PackingRecipeTransferInfo() {
        super(PackingMenu.class, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE, 0, PackageContents.SLOTS, PackageContents.SLOTS, 36);
    }

    @Override
    public boolean requireCompleteSets(PackingMenu container, RecipeHolder<MailRecipe> recipe) {
        // This method replaces logic that was previously done using a mixin and is now crashing with new JEI versions.
        // Instead of fixing that mixin and thus breaking compatibility with older JEI versions, we do it here.
        // This is not great either, as it's causing a side effect in a method implying that we're only checking something,
        // but it's called in the same place (where mixin was previously targeting) and nowhere else (at least at the time of writing).
        ServiceAddress address = recipe.value().getAddress();
        container.presetAddress(address);
        new ServerboundPackingMenuPresetAddressPacket(Optional.ofNullable(address)).sendToServer();

        return super.requireCompleteSets(container, recipe);
    }
}
