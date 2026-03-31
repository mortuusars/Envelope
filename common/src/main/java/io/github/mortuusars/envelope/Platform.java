package io.github.mortuusars.envelope;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class Platform {
    @ExpectPlatform
    public static boolean isInDevEnv() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static @Nullable MinecraftServer getCurrentServer() {
        throw new AssertionError();
    }

    public static Optional<MinecraftServer> getServer() {
        return Optional.ofNullable(getCurrentServer());
    }

    public static @NotNull MinecraftServer getCurrentServerOrThrow() {
        return Objects.requireNonNull(getCurrentServer());
    }

    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoading(String modId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openMenu(ServerPlayer serverPlayer, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getGameDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerServiceDropOffHandlers() {
        throw new AssertionError();
    }

    // -- Events

    @ExpectPlatform
    public static MailDropOffResult postHandleMailDropOffEvent(MailDropOffContext context) {
        throw new AssertionError();
    }
}
