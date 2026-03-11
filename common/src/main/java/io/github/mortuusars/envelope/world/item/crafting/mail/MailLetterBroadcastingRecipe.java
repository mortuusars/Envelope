package io.github.mortuusars.envelope.world.item.crafting.mail;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class MailLetterBroadcastingRecipe extends MailCraftingRecipe {
    public MailLetterBroadcastingRecipe(EntityAddress address, NonNullList<Ingredient> ingredients, ItemStack result, float experience) {
        super(address, ingredients, result, experience);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.MAIL_BROADCASTING.get();
    }

    @Override
    public boolean onlyOneCraftPerDelivery() {
        return true;
    }

    @Override
    public ItemStack createOutput(PackageContents input, Address sender, ServerLevel level) {
        ItemStack inputLetter = ItemStack.EMPTY;
        LetterContent content = LetterContent.EMPTY;
        for (ItemStack stack : input.getItems()) {
            if (stack.is(Envelope.Tags.Items.LETTERS) && stack.has(Envelope.DataComponents.LETTER_CONTENT)) {
                content = stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY);
                inputLetter = stack;
                break;
            }
        }

        if (content.isEmpty()) {
            LogUtils.getLogger().error("Cannot broadcast a letter: no suitable letter was provided or the letter is missing a content. {}", input);

            Component reportText = Component.empty()
                  .append(Component.translatable("letter.envelope.broadcast_report.title").withStyle(ChatFormatting.ITALIC))
                  .append(Component.translatable("letter.envelope.broadcast_report.error"))
                  .append(Component.translatable("letter.envelope.broadcast_report.message",
                        Component.literal(Integer.toString(0)).withStyle(ChatFormatting.UNDERLINE)))
                  .append("\n\n")
                  .append(getAddress().getComponent());

            return Mail.createLetter(reportText)
                  .sender(getAddress())
                  .writeToLog(DeliveryRecord.sentFrom(getAddress()))
                  .get();
        } else {
            MailService service = MailService.of(level);

            ItemStack broadcastedLetter = Mail.createLetter(content)
                  .set(DataComponents.CUSTOM_NAME, inputLetter.get(DataComponents.CUSTOM_NAME))
                  .sender(sender)
                  .get();

            int count = 0;

            for (BlockAddress recipient : service.getKnownPlayers().getDefaultAddresses().values()) {
                if (recipient.equals(sender)) {
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
                  .append(Component.translatable("letter.envelope.broadcast_report.message",
                        Component.literal(Integer.toString(count)).withStyle(ChatFormatting.UNDERLINE)))
                  .append("\n\n")
                  .append(getAddress().getComponent());

            return Mail.createLetter(reportText)
                  .set(DataComponents.CUSTOM_NAME, Component.translatable("letter.envelope.broadcast_report.name"))
                  .sender(getAddress())
                  .get();
        }
    }
}
