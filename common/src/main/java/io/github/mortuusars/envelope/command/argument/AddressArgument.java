package io.github.mortuusars.envelope.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.world.mail.address.*;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AddressArgument implements ArgumentType<Address> {
    private final @Nullable Address.Type type;

    protected AddressArgument(@Nullable Address.Type type) {
        this.type = type;
    }

    public static AddressArgument all() {
        return new AddressArgument(null);
    }

    public static AddressArgument block() {
        return new AddressArgument(Address.Type.BLOCK);
    }

    public static AddressArgument player() {
        return new AddressArgument(Address.Type.PLAYER);
    }

    public static AddressArgument entity() {
        return new AddressArgument(Address.Type.ENTITY);
    }

    public static BlockAddress getBlock(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Address address = context.getArgument(name, Address.class);
        if (address instanceof BlockAddress block) {
            return block;
        }
        Component message = Component.literal("Address has wrong type: Expected: "
              + Address.Type.BLOCK.getSerializedName() + ", Got: "
              + address.getType().getSerializedName());
        throw new SimpleCommandExceptionType(message).create();
    }

    @Override
    public Address parse(StringReader reader) throws CommandSyntaxException {
        String id = reader.readString();

        Optional<Error> error = AddressValidation.id().test(id).getError();

        if (error.isPresent()) {
            Component message = Component.literal("Invalid address: ").append(error.get().getTranslation());
            throw new SimpleCommandExceptionType(message).create();
        }

        //TODO: this is not what it should be probably

        if (type == null) {
            return Address.UNKNOWN;
        }

        return switch (type) {
            case BLOCK -> new BlockAddress(id);
            case PLAYER -> new PlayerAddress(id);
            default -> throw new SimpleCommandExceptionType(Component.literal("Cannot parse address type " + type)).create();
        };
    }
}