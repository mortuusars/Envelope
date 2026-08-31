package io.github.mortuusars.envelope.command;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import io.github.mortuusars.mortaar.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.cases.CourierDeliveryTests;
import io.github.mortuusars.envelope.util.bugger.cases.MailCraftingRecipeTests;
import io.github.mortuusars.envelope.util.bugger.cases.MailCraftingTests;
import io.github.mortuusars.envelope.util.bugger.cases.StackIngredientTests;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.mortaar.bugger.test.TestResults;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

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
              .add(new CourierDeliveryTests(context.getSource().getServer()))
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

        /* Structure test
        List<BlockPos> positions = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            BlockPos pos = locateStructure(context.getSource(), ResourceKey.create(Registries.STRUCTURE, Envelope.resource("dovecote_plains")));
            if (pos.equals(BlockPos.ZERO)) {
                continue;
            }

            positions.add(pos);
        }

        if (positions.size() < 2) {
            context.getSource().sendFailure(Component.literal("Too few positions"));
            return 0;
        }

        List<Integer> lowestDistances = new ArrayList<>();
        int totalDistance = 0;

        for (BlockPos position : positions) {
            int lowest = Integer.MAX_VALUE;
            for (BlockPos pos : positions) {
                if (!pos.equals(position)) {
                    int distance = (int) Math.sqrt(position.distSqr(pos));
                    if (distance < lowest) {
                        lowest = distance;
                    }
                }
            }
            lowestDistances.add(lowest);
            totalDistance += lowest;
        }

        double average = (double) totalDistance / positions.size();


        boolean a = true;*/


//        Component text = Component.literal("       Report to Chief\n\n  Patrol we sent to village not return. Unexpected danger possible.\n\n  Must know more. Asking reinforcements.")
//              .setStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt")));
//
//        ItemStack letter = Mail.createLetter(text)
//              .set(DataComponents.ITEM_NAME, Component.literal("Report")
//                    .withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt")))).get();
//
//        player.addItem(letter);


//        player.level().getEntitiesOfClass(Pigeon.class, player.getBoundingBox().inflate(16))
//              .forEach(p -> p.restrictTo(player.blockPosition(), 4));
//        player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16))
//              .forEach(p -> p.restrictTo(player.blockPosition(), 4));

        //        EquineAssuranceBureau.sendNotice(MailService.of(player.serverLevel()),
//              ServiceAddress.getOrThrow(player.registryAccess(), ServiceAddresses.EQUINE_ASSURANCE_BUREAU));

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

    private static BlockPos locateStructure(CommandSourceStack source, ResourceKey<Structure> key) throws CommandSyntaxException {
        Registry<Structure> registry = source.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder.Reference<Structure> holder = registry.getHolderOrThrow(key);
        HolderSet.Direct<Structure> holderSet = HolderSet.direct(holder);
        BlockPos blockPos = BlockPos.containing(source.getPosition());
        ServerLevel serverLevel = source.getLevel();
        Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
        Pair<BlockPos, Holder<Structure>> pair = serverLevel.getChunkSource().getGenerator()
              .findNearestMapStructure(serverLevel, holderSet, blockPos, 100, true);
        stopwatch.stop();

        if (pair == null) {
            source.sendFailure(Component.literal("Cannot find " + key.location()));
            return BlockPos.ZERO;
        } else {
            BlockPos pos = pair.getFirst();

            Component component = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ()))
                  .withStyle(
                        style -> style.withColor(ChatFormatting.GREEN)
                              .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                  );

            source.sendSuccess(() -> component, true);
            return pos;
        }
    }
}
