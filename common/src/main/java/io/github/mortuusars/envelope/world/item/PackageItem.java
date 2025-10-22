package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.inventory.PackageMenu;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PackageItem extends BlockItem {
    public PackageItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (!stack.has(Envelope.DataComponents.PACKAGE_CONTENTS)) {
            return Component.translatable("item.envelope.package.empty");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (stack.has(Envelope.DataComponents.PACKAGE_LETTER)) {
            tooltipComponents.add(Component.translatable(
                    "gui.envelope.package.tooltip.has_letter" + (Screen.hasShiftDown() ? ".shift" : ""))
                    .withStyle(Style.EMPTY.withColor(0xFFD2BCA4)));
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.PACKAGE_CONTENTS));
    }

    // --

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        StoredItemStack storedLetter = stack.getOrDefault(Envelope.DataComponents.PACKAGE_LETTER, StoredItemStack.EMPTY);

        if (other.is(Envelope.Tags.Items.LETTERS)) {
            stack.set(Envelope.DataComponents.PACKAGE_LETTER, new StoredItemStack(other));
            access.set(storedLetter.getCopy());
            player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1.1f);
            return true;
        } else if (!storedLetter.isEmpty() && other.isEmpty()) {
            stack.remove(Envelope.DataComponents.PACKAGE_LETTER);
            access.set(storedLetter.getCopy());
            player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 0.8f);
            return true;
        }

        return false;
    }

    // --

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlatformHelper.openMenu(serverPlayer,
                    new SimpleMenuProvider((id, inventory, pl) ->
                            new PackageMenu(id, inventory, usedHand), player.getItemInHand(usedHand).getHoverName()),
                    buffer -> buffer.writeEnum(usedHand));
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }

    // --

    public int getTimesPacked(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 0);
    }

    public int getRemainingPacks(ItemStack stack) {
        return Config.Server.PACKAGE_PACK_LIMIT.get() - getTimesPacked(stack);
    }

    public boolean canPack(ItemStack stack) {
        return getTimesPacked(stack) < Config.Server.PACKAGE_PACK_LIMIT.get();
    }

    public boolean shouldBeDestroyedWhenEmpty(ItemStack stack) {
        return !canPack(stack);
    }

    public List<ItemStack> unpack(ItemStack stack) {
        List<ItemStack> items = new ArrayList<>();

        @Nullable StoredItemStack storedLetter = stack.get(Envelope.DataComponents.PACKAGE_LETTER);
        if (storedLetter != null) {
            items.add(storedLetter.getCopy());
            stack.remove(Envelope.DataComponents.PACKAGE_LETTER);
        }

        PackageContents contents = stack.getOrDefault(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.EMPTY);
        if (!contents.isEmpty()) {
            items.addAll(contents.copyItems());
            stack.remove(Envelope.DataComponents.PACKAGE_CONTENTS);
        }

        return items;
    }
}
