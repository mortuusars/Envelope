package io.github.mortuusars.envelope.util.bugger;

import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.BuggerPigeonDeliveryS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.BuggerPigeonPigeonholeDataS2CP;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class BuggerPackets {
    public static void sendPigeonDelivery(Pigeon pigeon) {
        if (PlatformHelper.isInDevEnv() && pigeon.level() instanceof ServerLevel) {
            Packets.sendToAllClients(new BuggerPigeonDeliveryS2CP(pigeon.getId(), Optional.ofNullable(pigeon.getDelivery())));
        }
    }

    public static void sendPigeonPigeonholeData(Pigeon pigeon) {
        if (PlatformHelper.isInDevEnv() && pigeon.level() instanceof ServerLevel) {
            Packets.sendToAllClients(new BuggerPigeonPigeonholeDataS2CP(pigeon.getId(), pigeon.getPigeonholeHandler()));
        }
    }
}
