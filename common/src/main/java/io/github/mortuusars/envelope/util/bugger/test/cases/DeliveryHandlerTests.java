package io.github.mortuusars.envelope.util.bugger.test.cases;

import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.DeliveryHandler;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class DeliveryHandlerTests extends BuggerTests {
    private final MinecraftServer server;

    public DeliveryHandlerTests(MinecraftServer server) {
        this.server = server;
        add(new Test("tickDelivery_callsCallbacksProperly", this::testCallbacks));
    }

    private DataResult<Boolean> testCallbacks() {
        TestDeliveryHandler handler = new TestDeliveryHandler();
        Delivery delivery = new Delivery(
              Id.createUnsafe(0),
              Optional.empty(),
              Address.UNKNOWN,
              Address.UNKNOWN,
              ItemStack.EMPTY,
              DeliveryRoute.EMPTY,
              DeliveryPhase.STARTED,
              0,
              false
        );

        while (!delivery.isEnded()) {
            handler.tickDelivery(server.overworld(), delivery);
        }

        for (Map.Entry<DeliveryPhase, BitSet> check : handler.checks.entrySet()) {
            BitSet set = check.getValue();
            if (!set.get(0)) return DataResult.error(() -> check.getKey() + " - not called phaseStarted");
            if (!set.get(1)) return DataResult.error(() -> check.getKey() + " - not called phaseTicked");
            if (!set.get(2)) return DataResult.error(() -> check.getKey() + " - not called phaseCompleted");
        }

        if (!handler.endedCorrectly) {
            return DataResult.error(() -> "endDelivery was not called properly.");
        }

        return DataResult.success(true);
    }

    private static class TestDeliveryHandler implements DeliveryHandler {
        public final Map<DeliveryPhase, BitSet> checks = Util.make(new HashMap<>(), map -> {
            for (DeliveryPhase phase : DeliveryPhase.values()) {
                map.put(phase, new BitSet());
            }
        });
        public boolean endedCorrectly;

        @Override
        public void endDelivery(ServerLevel level, Delivery delivery) {
            if (delivery.getPhase() == DeliveryPhase.FINISHED) {
                endedCorrectly = true;
            }
        }

        @Override
        public CourierOrigin getOrigin() {
            return CourierOrigin.service();
        }

        @Override
        public void advancePhase(ServerLevel level, Delivery delivery) {
            DeliveryPhase nextPhase = delivery.getPhase().next();
            delivery.setPhaseAndResetProgress(nextPhase);
        }

        @Override
        public int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
            return DeliveryHandler.super.getPhaseDuration(level, delivery, phase) -1 ;
        }

        @Override
        public void phaseStarted(ServerLevel level, Delivery delivery) {
            checks.get(delivery.getPhase()).set(0);
        }

        @Override
        public void phaseTicked(ServerLevel level, Delivery delivery) {
            checks.get(delivery.getPhase()).set(1);
        }

        @Override
        public void phaseCompleted(ServerLevel level, Delivery delivery) {
            checks.get(delivery.getPhase()).set(2);
        }
    }
}
