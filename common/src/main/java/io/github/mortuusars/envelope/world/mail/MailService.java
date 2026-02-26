package io.github.mortuusars.envelope.world.mail;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.address.type.*;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackDepartment;
import io.github.mortuusars.envelope.world.block.mailbox.Mailboxes;
import io.github.mortuusars.envelope.world.KnownPlayers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
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
    protected final PaybackDepartment paybackDepartment;
    protected final DeliveryManager deliveryManager;

    protected @Nullable KnownPlayers knownPlayers;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    private MailService(ServerLevel level) {
        Preconditions.checkArgument(operatesIn(level), "MailService cannot exist in the '" + level.dimension().location() + "' dimension.");
        this.level = level;
        this.mailboxes = new Mailboxes(level);
        this.mailEntities = new MailEntities(this);
        this.paybackDepartment = new PaybackDepartment(this);
        this.deliveryManager = new DeliveryManager(this);
    }

    /**
     * It's intended to be created only once for a level. Use {@link MailService#of} to get the instance.
     */
    @ApiStatus.Internal
    public static MailService create(ServerLevel level) {
        MailService service = new MailService(level);
        service.initialize();
        return service;
    }

    private void initialize() {

    }

    public static MailService of(ServerLevel level) {
        return level.getEnvelopeMailService();
    }

    public static boolean operatesIn(ResourceKey<Level> dimension) {
        return dimension == Level.OVERWORLD;
    }

    public static boolean operatesIn(Level level) {
        return operatesIn(level.dimension());
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

    public PaybackDepartment getPaybackDepartment() {
        return paybackDepartment;
    }

    public @NotNull KnownPlayers getKnownPlayers() {
        return knownPlayers == null
              ? knownPlayers = KnownPlayers.get(level, "envelope_players")
              : knownPlayers;
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
              getKnownPlayers().getDefaultAddresses().keySet(),
              getMailEntities().getAllAddresses()
        );
    }
    public AllAddresses getKnownAddressesOfType(@Nullable Address.Type type) {
        if (type == null) {
            return getKnownAddresses();
        }
        return switch (type) {
            case BLOCK -> AllAddresses.blocks(getMailboxes().getAllAddresses());
            case PLAYER -> AllAddresses.players(getKnownPlayers().getDefaultAddresses().keySet());
            case ENTITY -> AllAddresses.entities(getMailEntities().getAllAddresses());
            case CUSTOM, UNKNOWN -> AllAddresses.EMPTY;
        };
    }

    public Optional<BlockAddress> getPlayerDefaultAddress(PlayerAddress playerAddress) {
        return Optional.ofNullable(getKnownPlayers().getDefaultAddresses().get(playerAddress));
    }

    public AddressLocation getLocationOf(Address address) {
        return switch (address) {
            case BlockAddress block -> getMailboxes().getPositionOf(block).map(AddressLocation::exact).orElse(AddressLocation.DEFAULT);
            case PlayerAddress player -> getKnownPlayers().getDefaultAddressOf(player).map(this::getLocationOf).orElse(AddressLocation.DEFAULT);
            case EntityAddress entity -> entity.getEntity().location();
            default -> AddressLocation.DEFAULT;
        };
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

    public Result<DeliveryManager.StartedDelivery> sendCourierDeathNotice(LivingEntity entity, Delivery delivery, DamageSource damageSource) {
        Address recipient = delivery.getSender();

        if (!(recipient instanceof BlockAddress) && !(recipient instanceof PlayerAddress)) {
            return Result.error("Cannot send death notice: recipient is not 'real' (player).");
        }

        ItemStack letter = createCourierDeathNoticeLetter(entity, delivery, damageSource);

        return getDeliveryManager().startService(Delivery.draft()
              .deliver(letter)
              .from(getAddress())
              .to(recipient));
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

    public EntityAddress getAddress() {
        return EntityAddress.getOrThrow(getLevel().registryAccess(), MailEntities.MAIL_SERVICE);
    }
}