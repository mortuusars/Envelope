package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.TestResults;
import io.github.mortuusars.envelope.util.bugger.test.cases.DeliveryExecutorTests;
import io.github.mortuusars.envelope.util.bugger.test.cases.MailCraftingRecipeTests;
import io.github.mortuusars.envelope.util.bugger.test.cases.MailCraftingTests;
import io.github.mortuusars.envelope.util.bugger.test.cases.StackIngredientTests;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class EnvelopeDebugCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("debug")
              .then(Commands.literal("expire_all_awaiting_payback")
                    .executes(EnvelopeDebugCommand::timeoutAllPaybackMail))
              .then(Commands.literal("run_tests")
                    .executes(EnvelopeDebugCommand::runBuggerTests))
              .then(Commands.literal("test")
                    .executes(EnvelopeDebugCommand::test));
    }

    private static int timeoutAllPaybackMail(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        MailService service = MailService.of(level);
        int returnedCount = service.getPaybackDepartment().returnAllAwaitingAsTimedOut();

        if (returnedCount > 0) {
            context.getSource().sendSuccess(() -> Component.literal("Returned " +
                  returnedCount + " mail awaiting payback."), true);
        } else {
            context.getSource().sendFailure(Component.literal("No mail awaiting payback is returned."));
        }

        return 0;
    }

    private static int runBuggerTests(CommandContext<CommandSourceStack> context) {
        TestResults testResults = new BuggerTests()
              .add(new StackIngredientTests(context.getSource().getServer()))
              .add(new DeliveryExecutorTests(context.getSource().getServer()))
              .add(new MailCraftingRecipeTests(context.getSource().getServer()))
              .add(new MailCraftingTests(context.getSource().getServer()))
              .run(count -> context.getSource().sendSuccess(() ->
                    Component.literal("Running " + count + " bugger tests."), true));

        context.getSource().sendSuccess(() -> Component.literal("Bugger tests finished:"), true);

        if (testResults.failed().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("All tests are passed!")
                  .withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Passed: " + testResults.passed().size() + "\n"), true);
            context.getSource().sendSuccess(() -> Component.literal("Failed: " + testResults.failed().size() + ":")
                  .withStyle(ChatFormatting.RED), true);

            testResults.failed().forEach(failedTest -> {
                context.getSource().sendSuccess(() -> Component.literal(" " + failedTest.name() + ": " + failedTest.error())
                      .withStyle(ChatFormatting.RED), true);
            });
        }

        return 0;
    }

    // --

    private static int test(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

//        ItemStack item = new ItemStack(Envelope.Items.LETTER.get());
//        item.set(Envelope.DataComponents.LETTER_CONTENT,
//              new LetterContent(Component.translatable("gui.abuseReport.comments").withColor(0xFFAA7733)));
//        Mail.writeToLog(item, DeliveryRecord.sentFrom(Address.UNKNOWN, 123141L),
//              DeliveryRecord.payback(Component.literal("Test"), DeliveryRecord.MessageType.POSITIVE));
//        Mail.setSender(item, Address.UNKNOWN);
//
//        player.drop(item, false);


//        List<RecipeHolder<MailRecipe>> recipes = player.level().getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get());
//
//        List<ItemStack> items = List.of(
//              new ItemStack(Envelope.Items.ADDRESS_TAG.get(), 3),
//              new ItemStack(Items.RED_DYE),
//              new ItemStack(Items.RED_DYE),
//              new ItemStack(Items.RED_DYE)
//        );
//
//        SimpleRecipeInput input = new SimpleRecipeInput(items);
//        @Nullable MailRecipe recipe = null;
//
//        for (RecipeHolder<MailRecipe> holder : recipes) {
//            if (holder.value().matches(input, player.level())) {
//                recipe = holder.value();
//            }
//        }

//        MailService.of(player.serverLevel()).getDeliveryManager()
//              .startService(Delivery.draft()
//                    .deliver(Mail.createPackage(new PackageContents(List.of(new ItemStack(Items.ACACIA_LOG))))
//                          .get())
//                    .from(new Address.Block("Blue"))
//                    .to(new Address.Block("Red"))
//                    .startAtPhase(DeliveryPhase.DISPATCHING_DELIVERY));

//        ItemStack pkg = new ItemStack(Envelope.Items.PACKAGE.get());
//        pkg.set(Envelope.DataComponents.PACKAGE_CONTENTS, new PackageContents(List.of(new ItemStack(Items.FEATHER, 5))));
//        pkg.set(Envelope.DataComponents.SENDER, new Address.Block("Original-Sender"));
//        pkg.set(Envelope.DataComponents.RECIPIENT, new Address.Block("Base"));
//        pkg.set(Envelope.DataComponents.PAYBACK, Payback.createOrDefault(List.of(
//              new RequestedItem(Items.EMERALD, 3), new RequestedItem(ItemTags.LOGS, 13))));
//
//        Mail mail = new Mail(pkg);
//
//        ItemStack paybackPackage = new ItemStack(Envelope.Items.PAYBACK_PACKING_BOX.get());
//        paybackPackage.set(Envelope.DataComponents.PAYBACK_SUBJECT, new StoredItemStack(mail.getItemCopy()));
//        paybackPackage.set(Envelope.DataComponents.SENDER, Address.MAIL_SERVICE);
//        paybackPackage.set(Envelope.DataComponents.RECIPIENT, mail.getRecipient());
//
//        Containers.dropItemStack(context.getSource().getLevel(), player.getX(), player.getY(), player.getZ(), paybackPackage);

        return 0;
    }
}
