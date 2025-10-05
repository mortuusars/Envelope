package io.github.mortuusars.envelope.util;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.*;

public class ItemAndStack<T extends Item> implements DataComponentHolder {
    private final T item;
    private final ItemStack stack;

    public ItemAndStack(ItemStack stack) {
        this.stack = stack;
        //noinspection unchecked
        this.item = (T) stack.getItem();
    }

    public T getItem() {
        return item;
    }

    public ItemStack getItemStack() {
        return stack;
    }

    public ItemAndStack<T> apply(BiConsumer<T, ItemStack> function) {
        function.accept(item, stack);
        return this;
    }

    public <R> R map(BiFunction<T, ItemStack, R> mappingFunction) {
        return mappingFunction.apply(item, stack);
    }

    // -- Components

    @Override
    public @NotNull DataComponentMap getComponents() {
        return stack.getComponents();
    }

    @Nullable
    public <C> C set(DataComponentType<? super C> component, @Nullable C value) {
        return stack.set(component, value);
    }

    @Nullable
    public <C, U> C update(DataComponentType<C> component, C defaultValue, U updateValue, BiFunction<C, U, C> updater) {
        return set(component, updater.apply(stack.getOrDefault(component, defaultValue), updateValue));
    }

    @Nullable
    public <C> C update(DataComponentType<C> component, C defaultValue, UnaryOperator<C> updater) {
        C object = getOrDefault(component, defaultValue);
        return set(component, updater.apply(object));
    }

    @Nullable
    public <C> C remove(DataComponentType<? extends C> component) {
        return stack.remove(component);
    }

    // --

    @Override
    public String toString() {
        return "ItemAndStack{" + stack.toString() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemAndStack<?> that = (ItemAndStack<?>) o;
        return Objects.equals(item, that.item) && Objects.equals(stack, that.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, stack);
    }
}
