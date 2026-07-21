package io.github.mortuusars.envelope.world.mail.service;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.Mailing;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.Optional;

public class EquineAssuranceBureau {
    private static final ResourceLocation RECIPE_ID = ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location()
          .withPrefix("mailing/")
          .withSuffix("/golden_horse_armor");
    private static final long MIN_SEND_INTERVAL = SharedConstants.TICKS_PER_GAME_DAY;
    private static final String DATA_LAST_SEND_TIME = "last_send_time";
    private static final String DATA_SENT_NOTICES_COUNT = "sent_notices_count";
    private static final String DATA_DELIVERIES_COUNT = "deliveries_count";

    public static void onTameAnimal(ServerPlayer player, Animal animal) {
        if (!Config.Server.SERVICE_EQUINE_BUREAU_NOTICE_SENDING_ENABLED.get() || !MailService.operatesIn(player.level())) {
            return;
        }

        Optional<ServiceAddress> bureauAddress = ServiceAddress.get(player.registryAccess(), ServiceAddresses.EQUINE_ASSURANCE_BUREAU);
        if (bureauAddress.isEmpty()) {
            return;
        }

        Optional<RecipeHolder<MailRecipe>> recipe = Mailing.getAllRecipesOf(bureauAddress.get(), player.serverLevel())
              .filter(recipeHolder -> recipeHolder.id().equals(RECIPE_ID))
              .findFirst();
        if (recipe.isEmpty()) {
            return;
        }

        if (!(animal instanceof AbstractHorse) && player.getRandom().nextInt(5) != 0) {
            return; // 1 in 5 chance of sending for animals other than horses.
        }

        MailService service = MailService.of(player.serverLevel());

        PlayerAddress playerAddress = new PlayerAddress(player);
        Optional<BlockAddress> defaultAddress = service.getPlayerDefaultAddress(playerAddress);
        if (defaultAddress.isEmpty()) {
            return;
        }

        CompoundTag data = service.getPersistentData().get(ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location());
        CompoundTag playerData = data.getCompound(player.getScoreboardName());

        long lastSendTime = playerData.getLong(DATA_LAST_SEND_TIME);
        if (lastSendTime > 0 && service.getGameTime() - lastSendTime < MIN_SEND_INTERVAL) {
            return;
        }

        int sentNoticesCount = playerData.getInt(DATA_SENT_NOTICES_COUNT);
        int deliveriesCount = playerData.getInt(DATA_DELIVERIES_COUNT);
        double chance = Math.max(0.02, 1.0 / (1 + ((sentNoticesCount + deliveriesCount) * 2)));

        if (service.getLevel().getRandom().nextDouble() < chance
              && sendLetter(player.serverLevel(), playerAddress)) {
            playerData.putInt(DATA_SENT_NOTICES_COUNT, sentNoticesCount + 1);
            playerData.putLong(DATA_LAST_SEND_TIME, service.getGameTime());
            data.put(player.getScoreboardName(), playerData);
            service.getPersistentData().set(ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location(), data);
        }
    }

    public static boolean sendLetter(ServerLevel level, Address recipient) {
        if (!MailService.operatesIn(level)) {
            return false;
        }

        ItemStack letter = createLetter(level);
        if (letter.isEmpty()) {
            return false;
        }

        return ServiceAddress.get(level.registryAccess(), ServiceAddresses.EQUINE_ASSURANCE_BUREAU)
              .map(address -> {
                  MailService.of(level).getDeliveryManager().startService(Delivery.draft()
                        .deliver(letter)
                        .from(address)
                        .to(recipient));
                  return true;
              })
              .orElse(false);
    }

    public static void onCraft(MailDropOffContext context, ServiceAddress address, MailDropOffResult craftingResult) {
        context.getDelivery().getOwner()
              .filter(owner -> isCarryingRecipeResult(context, craftingResult))
              .flatMap(owner -> context.getService().getKnownPlayers().getDataOf(owner))
              .ifPresent(player -> {
                  CompoundTag data = context.getService().getPersistentData().get(ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location());
                  CompoundTag playerData = data.getCompound(player.getProfile().getName());

                  playerData.putInt(DATA_DELIVERIES_COUNT, playerData.getInt(DATA_DELIVERIES_COUNT) + 1);
                  data.put(player.getProfile().getName(), playerData);

                  context.getService().getPersistentData().set(ServiceAddresses.EQUINE_ASSURANCE_BUREAU.location(), data);
              });
    }

    private static boolean isCarryingRecipeResult(MailDropOffContext context, MailDropOffResult craftingResult) {
        ItemStack expectedResult = context.getService().getLevel().getRecipeManager()
              .byKey(RECIPE_ID)
              .map(holder -> holder.value().getResultItem(context.getService().getLevel().registryAccess()))
              .orElse(new ItemStack(Items.BARRIER));

        ItemStack actualResult = craftingResult.getMail()
              .getOrDefault(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.EMPTY)
              .getItem(0);

        return ItemStack.isSameItemSameComponents(expectedResult, actualResult);
    }

    public static ItemStack createLetter(ServerLevel level) {
        Optional<RecipeHolder<MailRecipe>> recipe = ServiceAddress.get(level.registryAccess(), ServiceAddresses.EQUINE_ASSURANCE_BUREAU)
              .flatMap(address -> Mailing.getAllRecipesOf(address, level)
                    .filter(recipeHolder -> recipeHolder.id().equals(RECIPE_ID))
                    .findFirst());

        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SimpleContainer inputContainer = new SimpleContainer(PackageContents.SLOTS);

        recipe.get().value().getIngredients().stream()
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
