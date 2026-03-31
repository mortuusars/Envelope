package io.github.mortuusars.envelope.world.mail.service;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.Mailing;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class EquineAssuranceBureau {
    private static final ResourceLocation RECIPE_ID = Envelope.resource("mailing/equine_insurance_bureau/golden_horse_armor");

    public static void tick(MailService service, ServiceAddress address) {
        if (!Config.Server.SERVICE_EQUINE_BUREAU_NOTICE_SENDING_ENABLED.get()) {
            return;
        }

        long currentTime = service.getGameTime();
        CompoundTag data = service.getPersistentData().get(ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location());
        long lastSendTime = data.getLong("last_send_time");

        long days = service.getLevel().getDayTime() / 24000L;
        long interval = Ticks.fromMinutes(Config.Server.SERVICE_EQUINE_BUREAU_NOTICE_SENDING_BASE_INTERVAL_MINUTES.get())
              + days * Ticks.fromMinutes(Config.Server.SERVICE_EQUINE_BUREAU_NOTICE_SENDING_ADDITIONAL_INTERVAL_PER_DAY_MINUTES.get());

        if (currentTime - lastSendTime < interval) {
            return;
        }

        double chance = Config.Server.SERVICE_EQUINE_BUREAU_NOTICE_SENDING_CHANCE_PER_TICK.get();
        if (service.getLevel().getRandom().nextDouble() >= chance) {
            return;
        }

        if (sendNotice(service, address) > 0) {
            data.putLong("last_send_time", service.getGameTime());
            service.getPersistentData().set(ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location(), data);
        }
    }

    public static int sendNotice(MailService service, ServiceAddress address) {
        Map<PlayerAddress, BlockAddress> defaultAddresses = service.getKnownPlayers().getDefaultAddresses();
        if (defaultAddresses.isEmpty()) {
            return 0;
        }

        Optional<RecipeHolder<MailRecipe>> recipe = Mailing.getAllRecipesOf(address, service.getLevel())
              .filter(recipeHolder -> recipeHolder.id().equals(RECIPE_ID))
              .findFirst();
        if (recipe.isEmpty()) {
            return 0;
        }

        ItemStack letter = createLetter(recipe.get());

        int count = 0;

        for (PlayerAddress playerAddress : defaultAddresses.keySet()) {
            service.getDeliveryManager().startService(Delivery.draft()
                  .deliver(letter.copy())
                  .from(address)
                  .to(playerAddress));
            count++;
        }

        return count;
    }

    public static ItemStack createLetter(RecipeHolder<MailRecipe> recipe) {
        SimpleContainer inputContainer = new SimpleContainer(PackageContents.SLOTS);

        recipe.value().getIngredients().stream()
              .map(i -> Arrays.stream(i.getItems()).findFirst())
              .filter(Optional::isPresent)
              .forEach(item -> inputContainer.addItem(item.get()));

        MutableComponent requiredItems = Component.empty();
        boolean multiple = false;

        for (ItemStack stack : inputContainer.removeAllItems()) {
            Style style = createItemStyle(stack);

            if (multiple) {
                requiredItems.append(", ");
            }

            if (stack.getCount() > 1) {
                requiredItems.append(Component.literal(stack.getCount() + "x ").withStyle(style));
            }

            requiredItems.append(stack.getHoverName().copy().withStyle(style));

            multiple = true;
        }

        Component body = Component.translatable("letter.envelope.equine_assurance_notice.body", requiredItems);

        return Mail.createLetter(body)
              .set(DataComponents.ITEM_NAME, Component.translatable("letter.envelope.equine_assurance_notice.name"))
              .get();
    }

    private static Style createItemStyle(ItemStack stack) {
        return Style.EMPTY
              .withColor(ChatFormatting.DARK_RED)
              .withUnderlined(true)
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)));
    }
}
