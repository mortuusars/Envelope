package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MailRecipeInput implements RecipeInput {
    private final MailService service;
    private final Address sender;
    private final List<ItemStack> items;
    private StackedContents stackedContents;
    private int ingredientCount;

    public MailRecipeInput(MailService service, Address sender, List<ItemStack> items) {
        this.service = service;
        this.sender = sender;
        this.items = new ArrayList<>(items);
        updateIngredients();
    }

    public MailRecipeInput(MailService service, Address sender, PackageContents contents) {
        this(service, sender, contents.copyItems());
    }

    private void updateIngredients() {
        stackedContents = new StackedContents();
        int i = 0;
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                i++;
                this.stackedContents.accountStack(itemStack, 1);
            }
        }
        this.ingredientCount = i;
    }

    // --

    public MailService service() {
        return service;
    }

    public ServerLevel level() {
        return service.getLevel();
    }

    public Address sender() {
        return sender;
    }

    public List<ItemStack> items() {
        return items;
    }

    @Override
    public @NotNull ItemStack getItem(int index) {
        return items.get(index);
    }

    public void setItem(int index, ItemStack item) {
        items.set(index, item);
        updateIngredients();
    }

    @Override
    public int size() {
        return items.size();
    }

    public StackedContents stackedContents() {
        return stackedContents;
    }

    public int ingredientCount() {
        return ingredientCount;
    }

    public PackageContents toPackageContents() {
        return new PackageContents(items());
    }

    // --


    @Override
    public String toString() {
        return "MailingInput{" +
              "sender=" + sender +
              ", items=" + items +
              '}';
    }

    @SuppressWarnings("deprecation")
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        return object instanceof MailRecipeInput input
              && sender.equals(input.sender)
              && ingredientCount == input.ingredientCount
              && ItemStack.listMatches(items, input.items);
    }

    @SuppressWarnings("deprecation")
    public int hashCode() {
        return sender.hashCode() + ItemStack.hashStackList(this.items);
    }
}
