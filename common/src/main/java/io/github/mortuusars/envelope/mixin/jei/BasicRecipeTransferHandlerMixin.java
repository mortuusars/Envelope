package io.github.mortuusars.envelope.mixin.jei;

import io.github.mortuusars.envelope.network.packet.serverbound.ServerboundPackingMenuPresetAddressPacket;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BasicRecipeTransferHandler.class)
public abstract class BasicRecipeTransferHandlerMixin<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
    @Inject(method = "transferRecipe", at = @At(value = "INVOKE", target = "Lmezz/jei/common/network/IConnectionToServer;sendPacketToServer(Lmezz/jei/common/network/packets/PlayToServerPacket;)V"))
    private void onTransferRecipe(C container, R recipe, IRecipeSlotsView recipeSlotsView, Player player,
                                  boolean maxTransfer, boolean doTransfer, CallbackInfoReturnable<IRecipeTransferError> cir) {
        @Nullable Recipe<?> actualRecipe = null;

        if (recipe instanceof Recipe<?> direct) {
            actualRecipe = direct;
        } else if (recipe instanceof RecipeHolder<?> holder) {
            actualRecipe = holder.value();
        }

        if (container instanceof PackingMenu packingMenu && actualRecipe instanceof MailRecipe mailRecipe) {
            packingMenu.presetAddress(mailRecipe.getAddress());
            new ServerboundPackingMenuPresetAddressPacket(Optional.ofNullable(mailRecipe.getAddress())).sendToServer();
        }
    }
}
