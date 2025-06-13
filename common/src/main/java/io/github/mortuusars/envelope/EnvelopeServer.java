package io.github.mortuusars.envelope;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.world.KnownPlayers;
import io.github.mortuusars.envelope.world.mail.MailCoordinator;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnvelopeServer {
    private static boolean initialized = false;

    private static @Nullable MailCoordinator mailCoordinator = null;
    private static @Nullable KnownPlayers knownPlayers = null;

    public static boolean isInitialized() {
        return initialized;
    }

    public static void init(MinecraftServer server) {
        mailCoordinator = new MailCoordinator();
        knownPlayers = KnownPlayers.loadOrCreate(server);
        initialized = true;
    }

    public static void stop(MinecraftServer server) {
        mailCoordinator = null;
        knownPlayers = null;
        initialized = false;
    }

    // --

    public static @NotNull MailCoordinator getMailCoordinator() {
        Preconditions.checkNotNull(mailCoordinator, "Tried to retrieve MailCoordinator before server has initialized.");
        return mailCoordinator;
    }

    public static @NotNull KnownPlayers getKnownPlayers() {
        Preconditions.checkNotNull(knownPlayers, "Tried to retrieve KnownPlayers before server has initialized.");
        return knownPlayers;
    }

    // --

    public static void tick(MinecraftServer server) {
        if (isInitialized()) {
            getMailCoordinator().tick(server);
        }
    }
}
