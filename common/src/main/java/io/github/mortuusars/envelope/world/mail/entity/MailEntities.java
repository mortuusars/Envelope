package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MailEntities {
    public static final ResourceKey<MailEntity> MAIL_SERVICE = createKey(Envelope.resource("mail_service"));
    public static final ResourceKey<MailEntity> AUTOMATED_SUPPLY_SERVICE = createKey(Envelope.resource("automated_supply_service"));
    public static final ResourceKey<MailEntity> EQUINE_ASSURANCE_BUREAU = createKey(Envelope.resource("equine_insurance_bureau"));

    private final MailService service;

    public MailEntities(MailService service) {
        this.service = service;
    }

    public MailService getMailService() {
        return service;
    }

    public Set<EntityAddress> getAllAddresses() {
        return getMailService().getLevel().registryAccess().registryOrThrow(Envelope.Registries.MAIL_ENTITY)
              .holders()
              .map(EntityAddress::new)
              .collect(Collectors.toSet());
    }

    public void tick() {
        EntityAddress.get(getMailService().getLevel().registryAccess(), EQUINE_ASSURANCE_BUREAU)
              .ifPresent(address -> EquineAssuranceBureau.tick(getMailService(), address));
    }

    // --

    public static ResourceKey<MailEntity> createKey(ResourceLocation location) {
        return ResourceKey.create(Envelope.Registries.MAIL_ENTITY, location);
    }

    public static void bootstrap(BootstrapContext<MailEntity> context) {
        context.register(MAIL_SERVICE, new MailEntity(
              Component.translatable("address.envelope.mail_service"),
              EnvelopeSymbols.ADDRESS_MAIL_SERVICE,
              new AddressLocation.Relative(1000))); //TODO: use 0? mail hub
        context.register(AUTOMATED_SUPPLY_SERVICE, new MailEntity(
              Component.translatable("address.envelope.automated_supply_service"),
              EnvelopeSymbols.ADDRESS_ENTITY,
              new AddressLocation.Relative(2000)));
        context.register(EQUINE_ASSURANCE_BUREAU, new MailEntity(
              Component.translatable("address.envelope.equine_assurance_bureau"),
              EnvelopeSymbols.ADDRESS_ENTITY,
              new AddressLocation.Relative(2000)));
    }

    public @NotNull Holder<MailEntity> get(ResourceKey<MailEntity> key) {
        return getMailService().getLevel().registryAccess().registryOrThrow(Envelope.Registries.MAIL_ENTITY)
              .getHolderOrThrow(key);
    }

    public @NotNull EntityAddress getAddress(ResourceKey<MailEntity> key) {
        return new EntityAddress(get(key));
    }
}
