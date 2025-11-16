package io.github.mortuusars.envelope.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.mortuusars.envelope.util.validation.Issue;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressValidation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AddressArgument implements ArgumentType<Address> {
    private final @Nullable Address.Type type;

    protected AddressArgument(@Nullable Address.Type type) {
        this.type = type;
    }

    public static AddressArgument all() {
        return new AddressArgument(null);
    }

    public static AddressArgument pigeonhole() {
        return new AddressArgument(Address.Type.PIGEONHOLE);
    }

    public static AddressArgument player() {
        return new AddressArgument(Address.Type.PLAYER);
    }

    public static AddressArgument entity() {
        return new AddressArgument(Address.Type.ENTITY);
    }

    public static Address.Pigeonhole getPigeonhole(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Address argument = context.getArgument(name, Address.class);
        if (argument instanceof Address.Pigeonhole pigeonhole) return pigeonhole;
        throw new SimpleCommandExceptionType(Component.literal("Address has wrong type: Expected: "
              + Address.Type.PIGEONHOLE.getSerializedName() + ", Got: "
              + argument.type().getSerializedName())).create();
    }

    @Override
    public Address parse(StringReader reader) throws CommandSyntaxException {
        String id = reader.readString();

        List<Issue> issues = AddressValidation.checkFormat().validate(id);
        if (!issues.isEmpty()) {
            throw new SimpleCommandExceptionType(Component.literal("Invalid address: "
                  + issues.getFirst().getMessage().getString())).create();
        }

        return switch (type) {
            case PIGEONHOLE -> new Address.Pigeonhole(id);
            case PLAYER -> new Address.Player(id);
            case ENTITY -> new Address.Entity(id);
            case null -> new Address.Pigeonhole(id);
        };
    }
}