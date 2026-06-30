package io.github.mortuusars.envelope.neoforge.datagen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.PigeonVariant;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddresses;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BuiltInDatapackEntries extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder REGISTRIES = new RegistrySetBuilder()
          .add(Envelope.Registries.PIGEON_VARIANT, PigeonVariant::bootstrap)
          .add(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, ServiceAddresses::bootstrap)
          .add(Envelope.Registries.SEAL_MATERIAL, SealMaterial::bootstrap)
          .add(Envelope.Registries.SEAL_IMPRESSION, SealImpression::bootstrap);

    public BuiltInDatapackEntries(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider, REGISTRIES, Set.of(Envelope.ID));
    }
}