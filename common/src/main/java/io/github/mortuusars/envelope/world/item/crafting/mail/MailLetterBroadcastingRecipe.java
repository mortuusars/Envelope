package io.github.mortuusars.envelope.world.item.crafting.mail;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeCodecs;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class MailLetterBroadcastingRecipe extends CustomMailRecipe {
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result = Mail.createLetter(Component.empty())
          .set(DataComponents.ITEM_NAME, Component.translatable("letter.envelope.broadcast_report.name"))
          .get();

    public MailLetterBroadcastingRecipe(ServiceAddress address, NonNullList<Ingredient> ingredients) {
        super(address);
        this.ingredients = ingredients;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.MAIL_LETTER_BROADCASTING.get();
    }

    @Override
    public boolean isOneCraftPerDelivery() {
        return true; // Prevent spamming and spawning tons of couriers
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public @NotNull ItemStack assemble(MailRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public @NotNull ItemStack assembleWithSideEffects(MailRecipeInput input, HolderLookup.Provider registries) {
        ItemStack inputLetter = ItemStack.EMPTY;
        LetterContent content = LetterContent.EMPTY;
        for (ItemStack stack : input.items()) {
            if (stack.is(Envelope.Tags.Items.LETTERS) && stack.has(Envelope.DataComponents.LETTER_CONTENT)) {
                content = stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY);
                inputLetter = stack;
                break;
            }
        }

        MailService service = input.service();
        Map<PlayerAddress, BlockAddress> defaultAddresses = service.getKnownPlayers().getDefaultAddresses();

        if (content.isEmpty()) {
            LogUtils.getLogger().error("Cannot broadcast a letter: no suitable letter was provided or the letter is missing a content. {}", input);

            Component reportText = Component.empty()
                  .append(Component.translatable("letter.envelope.broadcast_report.title").withStyle(ChatFormatting.ITALIC))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.broadcast_report.error_no_message"))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.broadcast_report.total",
                        Component.literal(Integer.toString(0)).withStyle(ChatFormatting.UNDERLINE)))
                  .append("\n\n")
                  .append(getAddress().getComponent());

            return Mail.of(assemble(input, registries))
                  .set(Envelope.DataComponents.LETTER_CONTENT, new LetterContent(reportText))
                  .get();
        } else if (defaultAddresses.isEmpty()
              || (defaultAddresses.size() == 1 && defaultAddresses.values().stream().findFirst().get().equals(input.sender()))) {
            Component reportText = Component.empty()
                  .append(Component.translatable("letter.envelope.broadcast_report.title").withStyle(ChatFormatting.ITALIC))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.broadcast_report.error_no_recipients"))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.broadcast_report.total",
                        Component.literal(Integer.toString(0)).withStyle(ChatFormatting.UNDERLINE)))
                  .append("\n\n")
                  .append(getAddress().getComponent());

            return Mail.of(assemble(input, registries))
                  .set(Envelope.DataComponents.LETTER_CONTENT, new LetterContent(reportText))
                  .get();
        } else {
            ItemStack broadcastedLetter = Mail.createLetter(content)
                  .set(DataComponents.CUSTOM_NAME, inputLetter.get(DataComponents.CUSTOM_NAME))
                  .sender(input.sender())
                  .get();

            int count = 0;

            for (BlockAddress recipient : defaultAddresses.values()) {
                if (recipient.equals(input.sender())) {
                    continue;
                }

                service.getDeliveryManager().startService(Delivery.draft()
                      .deliver(broadcastedLetter.copy())
                      .from(service.getAddress())
                      .to(recipient));

                count++;
            }

            Component reportText = Component.empty()
                  .append(Component.translatable("letter.envelope.broadcast_report.title").withStyle(ChatFormatting.ITALIC))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.broadcast_report.success"))
                  .append("\n\n")
                  .append(Component.translatable("letter.envelope.broadcast_report.total",
                        Component.literal(Integer.toString(count)).withStyle(ChatFormatting.UNDERLINE)))
                  .append("\n\n")
                  .append(getAddress().getComponent());

            return Mail.of(assemble(input, registries))
                  .set(Envelope.DataComponents.LETTER_CONTENT, new LetterContent(reportText))
                  .get();
        }
    }

    public static class Serializer implements RecipeSerializer<MailLetterBroadcastingRecipe> {
        public static final MapCodec<MailLetterBroadcastingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              ServiceAddress.DEFINITION_CODEC
                    .fieldOf("address")
                    .forGetter(MailLetterBroadcastingRecipe::getAddress),
              EnvelopeCodecs.recipeIngredients(PackageContents.SLOTS, Envelope.RecipeTypes.MAILING.get())
                    .fieldOf("ingredients")
                    .forGetter(MailLetterBroadcastingRecipe::getIngredients)
        ).apply(i, MailLetterBroadcastingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MailLetterBroadcastingRecipe> STREAM_CODEC = StreamCodec.composite(
              ServiceAddress.STREAM_CODEC, MailLetterBroadcastingRecipe::getAddress,
              Ingredient.CONTENTS_STREAM_CODEC
                    .apply(ByteBufCodecs.list(PackageContents.SLOTS))
                    .map(list ->
                                NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)),
                          Function.identity()), MailLetterBroadcastingRecipe::getIngredients,
              MailLetterBroadcastingRecipe::new
        );

        @Override
        public @NotNull MapCodec<MailLetterBroadcastingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MailLetterBroadcastingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
