package io.github.mortuusars.envelope.network.packet;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public interface Packet extends CustomPacketPayload {
    boolean handle(PacketFlow flow, Player player);
}