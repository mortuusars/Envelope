package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class Addresses {
    public static Optional<BlockPos> getPosition(ServerLevel level, Address address) {
        if (address instanceof Address.Pigeonhole pigeonhole) {
            return PigeonholeNetwork.get(level).getPigeonholeData(pigeonhole).map(PigeonholeNetwork.PigeonholeData::getPosition);
        }
        return Optional.empty();
    }
}
