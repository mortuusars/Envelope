package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class PackageItem extends BlockItem implements Sealable {
    public PackageItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.PACKAGE_CONTENTS));
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
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack = stack.transmuteCopy(Envelope.Items.PACKING_BOX.get());
        Mail.setSender(stack, null);

        player.setItemInHand(hand, stack);

        ((PackingBoxItem)stack.getItem()).openPackingGui(player, hand, stack);

        return InteractionResultHolder.success(stack);
    }

    public List<ItemStack> unpack(ItemStack stack) {
        PackageContents contents = stack.getOrDefault(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.EMPTY);
        stack.remove(Envelope.DataComponents.PACKAGE_CONTENTS);
        return contents.copyItems();
    }

    // --

    @Override
    public ItemStack seal(Level level, ItemStack stack, Seal seal) {
        ItemStack sealedLetter = stack.transmuteCopy(Envelope.Items.SEALED_PACKAGE.get());
        sealedLetter.set(Envelope.DataComponents.SEAL, seal);
        return sealedLetter;
    }
}