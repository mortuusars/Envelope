package io.github.mortuusars.envelope.fabric;

import io.github.mortuusars.envelope.fabric.api.event.EnvelopeFabricEvents;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import io.netty.buffer.ByteBufUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

public class PlatformImpl {
    // Server field to access when no other objects are available to get it from.
    public static @Nullable MinecraftServer server = null;

    public static boolean isInDevEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return server;
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /**
     * This method is here because on forge checking if mod is loaded at mixin apply time is different (LoadingModList vs ModList)
     * But on fabric we can use the same code.
     */
    public static boolean isModLoading(String modId) {
        return isModLoaded(modId);
    }

    public static void openMenu(ServerPlayer serverPlayer, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        ExtendedScreenHandlerFactory<byte[]> extendedScreenHandlerFactory = new ExtendedScreenHandlerFactory<>() {
            @Override
            public byte[] getScreenOpeningData(ServerPlayer player) {
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(PacketByteBufs.create(), player.registryAccess());
                extraDataWriter.accept(buffer);
                byte[] bytes = ByteBufUtil.getBytes(buffer);
                buffer.release();
                return bytes;
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
                return menuProvider.createMenu(i, inventory, player);
            }

            @Override
            public @NotNull Component getDisplayName() {
                return menuProvider.getDisplayName();
            }
        };

        serverPlayer.openMenu(extendedScreenHandlerFactory);
    }

    public static Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void registerServiceDropOffHandlers() {

    }

    // -- Events

    public static MailDropOffResult postHandleMailDropOffEvent(MailDropOffContext context) {
        return EnvelopeFabricEvents.HANDLE_MAIL_DROP_OFF.invoker().handle(context);
    }
}
