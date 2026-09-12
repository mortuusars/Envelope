package io.github.mortuusars.envelope.world.mail.service.cloud_depository;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddresses;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CloudDepository {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation LETTER_MEANING_WITHDRAWAL_REQUEST = Envelope.resource("cloud_depository/withdrawal_request");
    public static final ResourceLocation LETTER_MEANING_STATUS_REQUEST = Envelope.resource("cloud_depository/status_request");
    public static final ResourceLocation LETTER_MEANING_EXPANSION_REQUEST = Envelope.resource("cloud_depository/expansion_request");

    public static final Component RETURN_MESSAGE_NO_IDENTITY = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.no_identity").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_NON_SERVICEABLE_IDENTITY = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.non_serviceable_identity").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_STORAGE_IS_FULL = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.storage_is_full").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_STORAGE_IS_EMPTY = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.storage_is_empty").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_INDEX_NOT_FOUND = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.index_not_found").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_CAPACITY_ALREADY_AT_MAX = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.capacity_already_at_max").withColor(Colors.TOOLTIP_RED);
    public static final Component LOG_MESSAGE_DEPOSITED = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.deposited").withColor(Colors.TOOLTIP_GREEN);
    public static final Component LOG_MESSAGE_WITHDRAWN = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.withdrawn").withColor(Colors.TOOLTIP_GREEN);

    protected final MailService service;
    protected final ServiceAddress address;
    protected final Seal seal;
    protected CloudDepositoryData data = null;

    public CloudDepository(MailService service) {
        this.service = service;
        this.address = ServiceAddress.getOrThrow(service.getLevel().registryAccess(), ServiceAddresses.CLOUD_DEPOSITORY);
        this.seal = createSeal(service.getLevel().registryAccess());
    }

    public MailService getService() {
        return service;
    }

    public @NotNull CloudDepositoryData getData() {
        if (data == null) {
            data = service.getPersistentData().getOrCreateTyped(CloudDepositoryData.TYPE);
        }
        return data;
    }

    public ServiceAddress getAddress() {
        return address;
    }

    public Seal getSeal() {
        return seal;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CloudDepositoryData.Account accountData : getData().accounts().values()) {
            count += accountData.getTotal();
        }
        return count;
    }

    // --

    public static MailDropOffResult handleDropOff(MailDropOffContext context) {
        return context.getService().getCloudService().handleDelivery(context);
    }

    public MailDropOffResult handleDelivery(MailDropOffContext context) {
        // Should be handled higher up the chain, but just to be safe:
        if (context.isReturned()) {
            LOGGER.info("'{}' received returned mail. Delivery: '{}'. Voiding.", context.getTarget(), context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        ItemStack mail = context.getMail();

        if (!Config.Server.SERVICE_CLOUD_DEPOSITORY_ENABLED.get()) {
            return MailDropOffResult.returned(mail, DeliveryRecord.Message.UNAVAILABLE);
        }

        if (!(mail.get(Envelope.DataComponents.SEAL) instanceof Seal mailSeal)
              || mailSeal.signature().getString().isEmpty()) {
            return MailDropOffResult.returned(mail, RETURN_MESSAGE_NO_IDENTITY);
        }

        String account = mailSeal.signature().getString();

        if (Config.Server.SERVICE_CLOUD_DEPOSITORY_ONLY_PLAYERS.get()
              && service.getKnownPlayers().getDataOf(account).isEmpty()) {
            return MailDropOffResult.returned(mail, RETURN_MESSAGE_NON_SERVICEABLE_IDENTITY);
        }

        if (mail.is(Envelope.Items.SEALED_LETTER.get())
              && mail.get(Envelope.DataComponents.LETTER_MEANING) instanceof ResourceLocation meaning) {
            if (meaning.equals(LETTER_MEANING_WITHDRAWAL_REQUEST)) {
                return handleWithdrawalRequest(context, account);
            }
            if (meaning.equals(LETTER_MEANING_STATUS_REQUEST)) {
                return handleStatusRequest(context, account);
            }
            if (meaning.equals(LETTER_MEANING_EXPANSION_REQUEST)) {
                return handleExpansionRequest(context, account);
            }
            return MailDropOffResult.returned(mail, DeliveryRecord.Message.REJECTED);
        }

        return handleItemDeposit(context, account);
    }

    protected MailDropOffResult handleItemDeposit(MailDropOffContext context, String account) {
        ItemStack mail = context.getMail();
        CloudDepositoryData.Account accountData = getData().ofAccount(account);

        int capacity = accountData.getCapacity();
        if (accountData.getTotal() >= capacity) {
            return MailDropOffResult.returned(mail, RETURN_MESSAGE_STORAGE_IS_FULL);
        }

        Mail.writeToLog(mail,
              DeliveryRecord.arrivedTo(getAddress()),
              DeliveryRecord.message(LOG_MESSAGE_DEPOSITED));

        accountData.getItems().add(mail);
        getData().setDirty();

        LOGGER.debug("{} has been put into storage: {}", mail, account);

        ItemStack reply = createDepositReportLetter(getSeal(), mail, accountData.getTotal(), capacity);
        return MailDropOffResult.reply(reply);
    }

    protected MailDropOffResult handleWithdrawalRequest(MailDropOffContext context, String account) {
        LOGGER.debug("Received withdrawal request from: {}", account);

        CloudDepositoryData.Account accountData = getData().ofAccount(account);

        if (accountData.getItems().isEmpty()) {
            return MailDropOffResult.returned(context.getMail(), RETURN_MESSAGE_STORAGE_IS_EMPTY);
        }

        if (context.getMail().get(DataComponents.CUSTOM_NAME) instanceof Component index) {
            ListIterator<ItemStack> iterator = accountData.getItems().listIterator(accountData.getItems().size());

            while(iterator.hasPrevious()) {
                ItemStack item = iterator.previous();
                if (index.equals(item.get(DataComponents.CUSTOM_NAME))) {
                    iterator.remove();
                    getData().setDirty();

                    LOGGER.debug("Withdrew indexed [{}] from: {}", account, item);

                    return MailDropOffResult.reply(Mail.of(item)
                          .sender(getAddress())
                          .recipient(context.getOrigin())
                          .writeToLog(DeliveryRecord.message(LOG_MESSAGE_WITHDRAWN)).get());
                }
            }

            return MailDropOffResult.returned(context.getMail(), RETURN_MESSAGE_INDEX_NOT_FOUND);
        }

        ItemStack item = accountData.getItems().removeLast();
        getData().setDirty();

        LOGGER.debug("Withdrew [{}] from: {}", account, item);

        return MailDropOffResult.reply(Mail.of(item)
              .sender(getAddress())
              .recipient(context.getOrigin())
              .writeToLog(DeliveryRecord.message(LOG_MESSAGE_WITHDRAWN)).get());
    }

    protected MailDropOffResult handleStatusRequest(MailDropOffContext context, String account) {
        LOGGER.debug("Received status request from: {}", account);

        CloudDepositoryData.Account accountData = getData().ofAccount(account);

        ItemStack report = createStatusReportLetter(seal, account, accountData.getTotal(), accountData.getCapacity(),
              Config.Server.SERVICE_CLOUD_DEPOSITORY_ACCOUNT_STORAGE_MAX_CAPACITY.get(),
              accountData.getItems(), context.getLevel().getRandom());
        return MailDropOffResult.reply(Mail.of(report)
              .sender(getAddress())
              .recipient(context.getOrigin())
              .get());
    }

    protected MailDropOffResult handleExpansionRequest(MailDropOffContext context, String account) {
        LOGGER.debug("Received expansion request from: {}", account);

        int maxCapacity = Config.Server.SERVICE_CLOUD_DEPOSITORY_ACCOUNT_STORAGE_MAX_CAPACITY.get();
        int increase = Config.Server.SERVICE_CLOUD_DEPOSITORY_ACCOUNT_STORAGE_CAPACITY_PER_EXPANSION.get();

        CloudDepositoryData.Account accountData = getData().ofAccount(account);
        int oldCapacity = accountData.getCapacity();

        if (accountData.getCapacity() >= maxCapacity) {
            return MailDropOffResult.returned(context.getMail(), RETURN_MESSAGE_CAPACITY_ALREADY_AT_MAX);
        }

        int newCapacity = Mth.clamp(accountData.getCapacity() + increase, 1, maxCapacity);
        accountData.setCapacity(newCapacity);
        getData().setDirty();

        LOGGER.debug("Increased storage capacity of account '{}' to {}", account, maxCapacity);

        int increasedBy = newCapacity - oldCapacity;

        ItemStack report = createExpansionReportLetter(seal, account, accountData.getCapacity(), maxCapacity, increasedBy);
        return MailDropOffResult.reply(Mail.of(report)
              .sender(getAddress())
              .recipient(context.getOrigin())
              .get());
    }

    // --

    public static @NotNull Seal createSeal(RegistryAccess registryAccess) {
        return new Seal(
              SealMaterial.getOrThrow(registryAccess, SealMaterial.WAX),
              SealSymbol.getOrThrow(registryAccess, SealSymbol.CLOUD_DEPOSITORY),
              Component.translatable("address.envelope.cloud_depository")
        );
    }

    public static ItemStack createDepositReportLetter(Seal seal, ItemStack depositedItem, int accountStorageCurrent, int accountStorageCapacity) {
        depositedItem = Mail.removeAllDeliveryData(depositedItem.copy());

        Component depositedItemComponent = !depositedItem.isEmpty()
              ? depositedItem.getHoverName().plainCopy().withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withUnderlined(true).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(depositedItem))))
              : Component.translatable("letter.envelope.cloud_depository.deposit_report.item").withStyle(ChatFormatting.DARK_RED);

        ItemStack withdrawalRequestLetter = createWithdrawalRequestLetter();
        Component withdrawalRequestComponent = Component.translatable("letter.envelope.cloud_depository.withdrawal_request.name").withStyle(Style.EMPTY
              .withColor(ChatFormatting.DARK_BLUE)
              .withUnderlined(true)
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(withdrawalRequestLetter))));

        return Mail.createSealedLetter(Component.translatable("letter.envelope.cloud_depository.deposit_report.message",
                    depositedItemComponent,
                    withdrawalRequestComponent,
                    Component.literal(accountStorageCurrent + "/" + accountStorageCapacity).withStyle(ChatFormatting.DARK_RED)))
              .itemName(Component.translatable("letter.envelope.cloud_depository.deposit_report.name"))
              .set(Envelope.DataComponents.SEAL, seal)
              .get();
    }

    public static ItemStack createStatusReportLetter(Seal seal, String account, int accountStorageCurrent, int accountStorageCapacity,
                                                     int maxCapacity, List<ItemStack> storedItems, RandomSource random) {
        List<ItemStack> storedIndexableItems = storedItems.stream()
              .filter(i -> i.has(DataComponents.CUSTOM_NAME))
              .toList();

        Component indexable;
        if (storedIndexableItems.isEmpty()) {
            indexable = Component.translatable("letter.envelope.cloud_depository.status_report.indexable_none");
        } else {
            List<ItemStack> indexablePool = new ArrayList<>(storedIndexableItems);
            MutableComponent randomIndexableItems = Component.empty();
            boolean hasElements = false;

            for (int i = 0; i < Math.min(storedIndexableItems.size(), 3); i++) {
                int randomIndex = random.nextInt(indexablePool.size());
                ItemStack item = indexablePool.remove(randomIndex);
                if (hasElements) {
                    randomIndexableItems.append(", ");
                }
                MutableComponent itemName = item.getHoverName().plainCopy().withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withUnderlined(true).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item))));
                randomIndexableItems.append(itemName);
                hasElements = true;
            }

            indexable = Component.translatable("letter.envelope.cloud_depository.status_report.indexable_items",
                  Component.literal(Integer.toString(storedIndexableItems.size())).withStyle(ChatFormatting.DARK_RED),
                  randomIndexableItems);
        }

        Component expansion = accountStorageCapacity < maxCapacity
              ? Component.translatable("letter.envelope.cloud_depository.status_report.expansion",
                    Component.literal(Integer.toString(maxCapacity)).withStyle(ChatFormatting.DARK_RED))
              : CommonComponents.EMPTY;

        return Mail.createSealedLetter(Component.translatable("letter.envelope.cloud_depository.status_report.message",
                    Component.literal(account).withStyle(ChatFormatting.DARK_RED),
                    Component.literal(accountStorageCurrent + "/" + accountStorageCapacity).withStyle(ChatFormatting.DARK_RED),
                    indexable, expansion))
              .itemName(Component.translatable("letter.envelope.cloud_depository.status_report.name"))
              .set(Envelope.DataComponents.SEAL, seal)
              .get();
    }

    public static ItemStack createExpansionReportLetter(Seal seal, String account, int accountStorageCapacity, int maxCapacity, int increasedBy) {
        Component further = accountStorageCapacity < maxCapacity
              ? Component.translatable("letter.envelope.cloud_depository.expansion_report.further_expansion",
                    Component.literal(Integer.toString(maxCapacity)).withStyle(ChatFormatting.DARK_RED))
              : CommonComponents.EMPTY;

        return Mail.createSealedLetter(Component.translatable("letter.envelope.cloud_depository.expansion_report.message",
                    Component.literal(account).withStyle(ChatFormatting.DARK_RED),
                    Component.literal(Integer.toString(increasedBy)).withStyle(ChatFormatting.DARK_RED),
                    Component.literal(Integer.toString(accountStorageCapacity)).withStyle(ChatFormatting.DARK_RED),
                    further))
              .itemName(Component.translatable("letter.envelope.cloud_depository.expansion_report.name"))
              .set(Envelope.DataComponents.SEAL, seal)
              .get();
    }

    public static ItemStack createWithdrawalRequestLetter() {
        return Mail.createLetter(Component.translatable("letter.envelope.cloud_depository.withdrawal_request.message"))
              .set(Envelope.DataComponents.LETTER_MEANING, CloudDepository.LETTER_MEANING_WITHDRAWAL_REQUEST)
              .get();
    }

    public static ItemStack createStatusRequestLetter() {
        return Mail.createLetter(Component.translatable("letter.envelope.cloud_depository.status_request.message"))
              .set(Envelope.DataComponents.LETTER_MEANING, CloudDepository.LETTER_MEANING_STATUS_REQUEST)
              .get();
    }

    public static ItemStack createExpansionRequestLetter() {
        return Mail.createLetter(Component.translatable("letter.envelope.cloud_depository.expansion_request.message"))
              .set(Envelope.DataComponents.LETTER_MEANING, CloudDepository.LETTER_MEANING_EXPANSION_REQUEST)
              .get();
    }
}
