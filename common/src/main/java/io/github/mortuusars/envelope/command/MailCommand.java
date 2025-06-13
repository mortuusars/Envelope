package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.api.mail.Mailbox;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MailCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mail")
                .requires((stack) -> stack.hasPermission(2))
                .then(Commands.literal("send")
                        .executes(MailCommand::sendTestMail)));
    }

    private static int sendTestMail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        Mail mail = new Mail(
                Mail.Sender.of(player),
                "Dev1",
                new ItemStack(Items.EMERALD),
                player.level().getGameTime(),
                100,
                Mail.Status.REGULAR);

        Mailbox.send(mail);

        context.getSource().sendSuccess(() -> Component.literal("Mail '" + mail + "' has been sent."), true);

        return 0;
    }
}
