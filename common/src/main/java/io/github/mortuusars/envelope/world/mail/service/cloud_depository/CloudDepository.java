package io.github.mortuusars.envelope.world.mail.service.cloud_depository;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.item.SealedItem;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CloudDepository {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation LETTER_MEANING_WITHDRAWAL_REQUEST = Envelope.resource("cloud_depository/withdrawal_request");
    public static final ResourceLocation LETTER_MEANING_STATUS_REQUEST = Envelope.resource("cloud_depository/status_request");

    public static final Component RETURN_MESSAGE_NO_IDENTITY = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.no_identity").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_STORAGE_IS_FULL = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.storage_is_full").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_STORAGE_IS_EMPTY = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.storage_is_empty").withColor(Colors.TOOLTIP_RED);
    public static final Component RETURN_MESSAGE_INDEX_NOT_FOUND = Component.translatable(
          "gui.envelope.delivery_log.message.cloud_depository.index_not_found").withColor(Colors.TOOLTIP_RED);
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

    // --

    public static MailDropOffResult handleDropOff(MailDropOffContext context) {
        return context.getService().getCloudService().handleDelivery(context);
    }

    public MailDropOffResult handleDelivery(MailDropOffContext context) {
        //TODO: Config

        // Should be handled higher up the chain, but just to be safe:
        if (context.isReturned()) {
            LOGGER.info("'{}' received returned mail. Delivery: '{}'. Voiding.", context.getTarget(), context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        ItemStack mail = context.getMail();

        if (!(mail.get(Envelope.DataComponents.SEAL) instanceof Seal mailSeal)
              || mailSeal.signature().getString().isEmpty()) {
            return MailDropOffResult.returned(mail, RETURN_MESSAGE_NO_IDENTITY);
        }

        String account = mailSeal.signature().getString();

        if (mail.is(Envelope.Items.SEALED_LETTER.get())
              && mail.get(Envelope.DataComponents.LETTER_MEANING) instanceof ResourceLocation meaning) {
            if (meaning.equals(LETTER_MEANING_WITHDRAWAL_REQUEST)) {
                return handleWithdrawalRequest(context, account);
            }
            if (meaning.equals(LETTER_MEANING_STATUS_REQUEST)) {
                return handleStatusRequest(context, account);
            }
            return MailDropOffResult.returned(mail, DeliveryRecord.Message.REJECTED);
        }

        return handleItemDeposit(context, account);
    }

    private MailDropOffResult handleItemDeposit(MailDropOffContext context, String account) {
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

    protected MailDropOffResult handleStatusRequest(MailDropOffContext context, String account) {
        LOGGER.debug("Received status request from: {}", account);

        CloudDepositoryData.Account accountData = getData().ofAccount(account);

        ItemStack statusReportLetter = createStatusReportLetter(seal, account,
              accountData.getTotal(), accountData.getCapacity(), accountData.getItems(), context.getLevel().getRandom());
        return MailDropOffResult.reply(Mail.of(statusReportLetter)
              .sender(getAddress())
              .recipient(context.getOrigin())
              .get());
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

                    return MailDropOffResult.reply(Mail.of(item)
                          .sender(getAddress())
                          .recipient(context.getOrigin())
                          .writeToLog(DeliveryRecord.message(LOG_MESSAGE_WITHDRAWN)).get());
                }
            }

            return MailDropOffResult.returned(context.getMail(), RETURN_MESSAGE_INDEX_NOT_FOUND);
        }

        ItemStack removedItem = accountData.getItems().removeLast();
        getData().setDirty();

        return MailDropOffResult.reply(Mail.of(removedItem)
              .sender(getAddress())
              .recipient(context.getOrigin())
              .writeToLog(DeliveryRecord.message(LOG_MESSAGE_WITHDRAWN)).get());
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CloudDepositoryData.Account accountData : getData().accounts().values()) {
            count += accountData.getTotal();
        }
        return count;
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
                    Component.literal(accountStorageCurrent + "/" + accountStorageCapacity)))
              .itemName(Component.translatable("letter.envelope.cloud_depository.deposit_report.name"))
              .set(Envelope.DataComponents.SEAL, seal)
              .get();
    }

    public static ItemStack createStatusReportLetter(Seal seal, String account, int accountStorageCurrent, int accountStorageCapacity,
                                                     List<ItemStack> storedItems, RandomSource random) {
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

        return Mail.createSealedLetter(Component.translatable("letter.envelope.cloud_depository.status_report.message",
                    Component.literal(account).withStyle(ChatFormatting.DARK_RED),
                    Component.literal(accountStorageCurrent + "/" + accountStorageCapacity).withStyle(ChatFormatting.DARK_RED),
                    indexable))
              .itemName(Component.translatable("letter.envelope.cloud_depository.status_report.name"))
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
}
