package io.github.mortuusars.envelope.world.mail.dropoff;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.crafting.mail.Mailing;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipeInput;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class ServiceCraftingDropOffHandler implements MailDropOffHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (!(context.getTarget() instanceof ServiceAddress address)) {
            return MailDropOffResult.PASS;
        }

        ItemStack mail = context.getMail();

        if (mail.isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        if (context.isReturned()) {
            LOGGER.info("Crafting '{}' received returned mail. Delivery: '{}'. Voiding", address, context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        Address sender = context.getDelivery().getSender();
        if (sender.equals(address)) {
            LOGGER.info("Service Crafting Handler cannot handle mail when sender is it's own address '{}'. Voiding.", context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        PackageContents packageContents = PackageContents.of(mail);
        if (packageContents.isEmpty()) {
            return MailDropOffResult.PASS;
        }

        MailRecipeInput input = new MailRecipeInput(context.getService(), sender, packageContents);

        @Nullable MailRecipe recipe = Mailing.findMatchingRecipe(address, input, context.getLevel())
              .map(RecipeHolder::value)
              .orElse(null);
        if (recipe == null) {
            return MailDropOffResult.PASS;
        }

        Mailing.Result result = Mailing.craft(recipe, input);

        List<ItemStack> results;

        if (result.output().size() == 1
              && result.output().getFirst().getCount() == 1
              && result.output().getFirst().is(Envelope.Tags.Items.MAILABLE)) {
            results = result.output();
        } else {
            results = Mail.createPackages(result.output(), pkg -> pkg.recipient(sender));
        }

        if (result.experience() > 0) {
            // It's not ideal that all experience is on one package.
            // But implementing it per package would be more complicated, and usually there's only one anyway.
            results.stream()
                  .filter(stack -> stack.getItem() instanceof PackageItem)
                  .findFirst()
                  .ifPresent(stack -> stack.set(Envelope.DataComponents.PACKAGE_EXPERIENCE, result.experience()));
        }

        return sendResults(context.getService(), address, mail, result.input().toPackageContents(), results, sender);
    }

    protected MailDropOffResult sendResults(MailService service, ServiceAddress address, ItemStack mail,
                                            PackageContents remainingInput, List<ItemStack> results, Address destination) {
        boolean hasRemainder = false;

        if (!remainingInput.isEmpty()) {
            ItemStack remainderPackage = mail.copy();
            remainderPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, remainingInput);
            results.addFirst(remainderPackage);
            hasRemainder = true;
        }

        if (results.isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        results.stream()
              .skip(1)
              .forEach(pkg -> {
                  service.getDeliveryManager()
                        .startService(Delivery.draft()
                              .deliver(pkg)
                              .from(address)
                              .to(destination));
              });

        if (hasRemainder) {
            return MailDropOffResult.returned(results.getFirst(), DeliveryRecord.Message.CRAFTING_UNPROCESSED_ITEMS);
        } else {
            return MailDropOffResult.reply(Mail.of(results.getFirst()).get());
        }
    }
}