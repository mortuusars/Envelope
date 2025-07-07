package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.*;
import io.github.mortuusars.envelope.world.mail.MailCoordinator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class MailCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mail")
                .requires((stack) -> stack.hasPermission(2))
                .then(Commands.literal("send")
                        .executes(MailCommand::sendTestMail)));
    }

    private static int sendTestMail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        ItemStack itemStack = new ItemStack(Envelope.Items.PACKAGE.get());

//        MailCoordinator.get(player.serverLevel().getServer()).getMailboxes().;

        Address recipient = new Address("dlh", Optional.empty());
        itemStack.set(Envelope.DataComponents.RECIPIENT, recipient);

        Mail mail = new Mail(
                new Address("test", Optional.empty()),
                recipient,
                itemStack,
                player.level().getGameTime(),
                30,
                Mail.Status.REGULAR);
//
        Mailbox.send(mail);
//
        context.getSource().sendSuccess(() -> Component.literal("Mail '" + mail + "' has been sent."), true);

        return 0;
    }
}
