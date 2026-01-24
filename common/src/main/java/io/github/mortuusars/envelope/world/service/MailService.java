package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.MailServiceEntity;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackDepartment;
import io.github.mortuusars.envelope.world.mail.receiver.EntityMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.MailboxMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PlayerMailReceiver;
import io.github.mortuusars.envelope.world.block.mailbox.Mailboxes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MailService {
    protected final ServerLevel level;

    protected final Mailboxes mailboxes;
    protected final MailEntities mailEntities;
    protected final MailServiceEntity mailServiceEntity;
    protected final PaybackDepartment paybackDepartment;
    protected final DeliveryManager deliveryManager;

    protected @Nullable Players players;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    private MailService(ServerLevel level) {
        Preconditions.checkArgument(level.dimension() == Level.OVERWORLD, "MailService can exist only on overworld level.");
        this.level = level;
        this.mailboxes = new Mailboxes(level);
        this.mailEntities = new MailEntities();
        this.mailServiceEntity = new MailServiceEntity(this);
        this.paybackDepartment = new PaybackDepartment(this);
        this.deliveryManager = new DeliveryManager(this);

        this.mailEntities.register(mailServiceEntity);
    }

    /**
     * It's intended to be created only once for a level. Use {@link MailService#of} to get the instance.
     */
    @ApiStatus.Internal
    public static MailService create(ServerLevel level) {
        return new MailService(level);
    }

    public static MailService of(ServerLevel level) {
        return level.getEnvelopeMailService();
    }

    // --

    public ServerLevel getLevel() {
        return level;
    }

    public Mailboxes getMailboxes() {
        return mailboxes;
    }

    public MailEntities getMailEntities() {
        return mailEntities;
    }

    public MailServiceEntity getMailService() {
        return mailServiceEntity;
    }

    public PaybackDepartment getPaybackDepartment() {
        return paybackDepartment;
    }

    public @NotNull Players getPlayers() {
        return players == null
              ? players = Players.get(level, "envelope_players")
              : players;
    }

    public @NotNull BackgroundDelivery getBackgroundDelivery() {
        return backgroundDelivery == null
              ? backgroundDelivery = BackgroundDelivery.get(level, "envelope_background_delivery")
              : backgroundDelivery;
    }

    public DeliveryManager getDeliveryManager() {
        return deliveryManager;
    }

    // -- Address

    public AllAddresses getKnownAddresses() {
        return new AllAddresses(
              getMailboxes().getAllAddresses(),
              getPlayers().getDefaultAddresses().keySet(),
              getMailEntities().getAllAddresses()
        );
    }

    public AllAddresses getKnownAddressesOfType(@Nullable Address.Type type) {
        if (type == null) {
            return getKnownAddresses();
        }
        return switch (type) {
            case BLOCK -> AllAddresses.blocks(getMailboxes().getAllAddresses());
            case PLAYER -> AllAddresses.players(getPlayers().getDefaultAddresses().keySet());
            case ENTITY -> AllAddresses.entities(getMailEntities().getAllAddresses());
        };
    }

    public Optional<Address.Block> getPlayerDefaultAddress(Address.Player playerAddress) {
        return Optional.ofNullable(getPlayers().getDefaultAddresses().get(playerAddress));
    }

    /**
     * @return "final" address. Mainly for getting default block address of a player.
     */
    public Address resolve(Address address) {
        if (address instanceof Address.Player playerAddress) {
            return getPlayerDefaultAddress(playerAddress).map(Address.class::cast).orElse(address);
        }
        return address;
    }

    public AddressLocation getLocationOf(Address address) {
        return address.map(
                    block -> getMailboxes().getPositionOf(block).map(AddressLocation::exact),
                    player -> getPlayers().getDefaultAddressOf(player).map(this::getLocationOf),
                    entity -> getMailEntities().byAddress(entity).map(MailEntity::getLocation))
              .orElse(AddressLocation.UNKNOWN);
    }

    // --

    public ItemStack deliverMail(Address address, ItemStack mail) {
        if (mail.isEmpty()) return mail;
        return address.map(MailboxMailReceiver::new, PlayerMailReceiver::new, EntityMailReceiver::new).receiveMail(level, mail);
    }

    // --

    public void tick() {
        getBackgroundDelivery().tick(level);
        getPaybackDepartment().tick();

        if (level.getGameTime() % 20 == 0) Bugger.MAIL_SERVICE.collectAndSendData(this);
    }

    // --

    public long getGameTime() {
        return getLevel().getGameTime();
    }

    public static boolean operatesIn(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    public Result<DeliveryManager.StartedDelivery> sendCourierDeathNotice(LivingEntity entity, Delivery delivery, DamageSource damageSource) {
        if (!(delivery.getSender() instanceof Address.Block address)) {
            return null;
        }

        ItemStack letter = createCourierDeathNoticeLetter(entity, delivery, damageSource);

        return getDeliveryManager().startService(Delivery.draft()
              .deliver(letter)
              .from(Address.MAIL_SERVICE)
              .to(address));
    }

    public ItemStack createCourierDeathNoticeLetter(LivingEntity entity, Delivery delivery, DamageSource damageSource) {
        Component text = Component.empty()
              .append(Component.translatable("letter.envelope.courier_death_notice.title").withStyle(ChatFormatting.ITALIC))
              .append(Component.translatable("letter.envelope.courier_death_notice.inform_" + entity.getRandom().nextInt(5),
                          damageSource.getLocalizedDeathMessage(entity)))
              .append(Component.translatable("letter.envelope.courier_death_notice.delivery." + delivery.getPhase().getSerializedName(),
                    delivery.getRecipient().format().withIcon().withIconColor(0xFFB5633F).withColor(0xFFB5633F).toComponent()))
              .append(!delivery.getMail().isEmpty()
                    ? Component.translatable("letter.envelope.courier_death_notice.mail_lost_" + entity.getRandom().nextInt(6),
                    delivery.getMail().getHoverName().copy()
                          .withStyle(Style.EMPTY.withUnderlined(true)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(delivery.getMail())))))
                    : CommonComponents.EMPTY)
              .append(Component.translatable("letter.envelope.courier_death_notice.condolence_" + entity.getRandom().nextInt(5),
                    entity.getName()))
              .append(Component.translatable("letter.envelope.courier_death_notice.signature"));

        return Mail.createLetter(text)
              .set(DataComponents.CUSTOM_NAME, Component.translatable("letter.envelope.courier_death_notice.name"))
              .get();
    }
}