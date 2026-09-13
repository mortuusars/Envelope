package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ServiceAddressTagsDatagen extends TagsProvider<ServiceAddressDefinition> {
    public ServiceAddressTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture,
                                     String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Envelope.Registries.SERVICE_ADDRESS_DEFINITION, completableFuture, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(Envelope.Tags.ServiceAddresses.HIDDEN)
              .add(ServiceAddress.EQUINE_ASSURANCE_BUREAU);
    }
}