package io.github.mortuusars.envelope.world.item.crafting.mail;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeCodecs;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class MailPaybackRequestCancelingRecipe extends CustomMailRecipe {
    public static final Logger LOGGER = LogUtils.getLogger();
    private final NonNullList<Ingredient> ingredients;

    public MailPaybackRequestCancelingRecipe(ServiceAddress address, NonNullList<Ingredient> ingredients) {
        super(address);
        this.ingredients = ingredients;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.MAIL_PAYBACK_REQUEST_CANCELING.get();
    }

    @Override
    public boolean isOneCraftPerDelivery() {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return Mail.of(new ItemStack(Envelope.Items.SEALED_PACKAGE.get()))
              .set(DataComponents.ITEM_NAME, Component.translatable("package.envelope.canceled_payback_request.name"))
              .get();
    }

    @Override
    public @NotNull ItemStack assemble(MailRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public @NotNull ItemStack assembleWithSideEffects(MailRecipeInput input, HolderLookup.Provider registries) {
        List<PaybackSubject> subjects = input.service().getPaybackDepartment().getSubjectsOf(input.sender())
              .stream()
              .sorted(Comparator.comparingLong(subject -> subject.id().getTick()))
              .toList();

        if (subjects.isEmpty()
              || !(input.service().getPaybackDepartment().removeSubject(subjects.getLast().id()) instanceof PaybackSubject subject)) {
            MutableComponent text = Component.empty()
                  .append(Component.translatable("letter.envelope.payback_request_cancel_report.title").withStyle(ChatFormatting.ITALIC))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.payback_request_cancel_report.error_no_requests"))
                  .append("\n\n")
                  .append(getAddress().getComponent());
            return Mail.createLetter(text)
                  .set(DataComponents.ITEM_NAME, Component.translatable("letter.envelope.payback_request_cancel_report.name"))
                  .get();
        }

        LOGGER.info("Payback request [{}] was cancelled.", subject);
        return Mail.returned(subject.mail(), DeliveryRecord.Message.PAYBACK_CANCELED);
    }

    public static class Serializer implements RecipeSerializer<MailPaybackRequestCancelingRecipe> {
        public static final MapCodec<MailPaybackRequestCancelingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              ServiceAddress.DEFINITION_CODEC
                    .fieldOf("address")
                    .forGetter(MailPaybackRequestCancelingRecipe::getAddress),
              EnvelopeCodecs.recipeIngredients(PackageContents.SLOTS, Envelope.RecipeTypes.MAILING.get())
                    .fieldOf("ingredients")
                    .forGetter(MailPaybackRequestCancelingRecipe::getIngredients)
        ).apply(i, MailPaybackRequestCancelingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MailPaybackRequestCancelingRecipe> STREAM_CODEC = StreamCodec.composite(
              ServiceAddress.STREAM_CODEC, MailPaybackRequestCancelingRecipe::getAddress,
              Ingredient.CONTENTS_STREAM_CODEC
                    .apply(ByteBufCodecs.list(PackageContents.SLOTS))
                    .map(list ->
                                NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)),
                          Function.identity()), MailPaybackRequestCancelingRecipe::getIngredients,
              MailPaybackRequestCancelingRecipe::new
        );

        @Override
        public @NotNull MapCodec<MailPaybackRequestCancelingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MailPaybackRequestCancelingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
