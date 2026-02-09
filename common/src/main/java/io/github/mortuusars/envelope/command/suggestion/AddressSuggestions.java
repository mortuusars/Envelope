package io.github.mortuusars.envelope.command.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class AddressSuggestions implements SuggestionProvider<CommandSourceStack> {
    private final @Nullable Address.Type type;

    protected AddressSuggestions(@Nullable Address.Type type) {
        this.type = type;
    }

    public static AddressSuggestions all() {
        return new AddressSuggestions(null);
    }

    public static AddressSuggestions block() {
        return new AddressSuggestions(Address.Type.BLOCK);
    }

    public static AddressSuggestions player() {
        return new AddressSuggestions(Address.Type.PLAYER);
    }

    public static AddressSuggestions entity() {
        return new AddressSuggestions(Address.Type.ENTITY);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MailService mailService = context.getSource().getLevel().getEnvelopeMailService();
        Stream<String> addresses = mailService.getKnownAddressesOfType(type).stream()
              .map(address -> address.getId().indexOf(" ") > 0 ? "\"" + address.getId() + "\"" : address.getId());
        return SharedSuggestionProvider.suggest(addresses, builder);
    }
}
