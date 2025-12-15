package io.github.mortuusars.envelope.command.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.EnvelopeContext;
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

    public static AddressSuggestions pigeonhole() {
        return new AddressSuggestions(Address.Type.BLOCK);
    }

    public static AddressSuggestions player() {
        return new AddressSuggestions(Address.Type.PLAYER);
    }

    public static AddressSuggestions entity() {
        return new AddressSuggestions(Address.Type.ENTITY);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        EnvelopeContext envelopeContext = context.getSource().getLevel().getEnvelopeContext();

        Stream<String> addresses = envelopeContext.addresses().getAll(type).stream().map(Address::id);

        return SharedSuggestionProvider.suggest(addresses, builder);
    }
}
