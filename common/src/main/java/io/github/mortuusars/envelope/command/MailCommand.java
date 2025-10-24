package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class MailCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("mail")
              .requires((stack) -> stack.hasPermission(2))
              .then(Commands.literal("send")
                    .then(Commands.argument("mail", ItemArgument.item(context))
                          .executes(stack -> sendMail(stack, ItemArgument.getItem(stack, "mail"))))));
    }

    private static int sendMail(CommandContext<CommandSourceStack> context, ItemInput item) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        ItemStack mail = item.createItemStack(1, false);

        try {
            BackgroundCourier courier = BackgroundCourier.virtual();
            courier.startDelivery(level, mail);
            level.getEnvelopeContext().getBackgroundDelivery().add(courier);
            context.getSource().sendSuccess(() -> Component.literal("Mail sent."), true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Cannot send: " + e.getMessage()));
        }

        return 0;
    }
}
