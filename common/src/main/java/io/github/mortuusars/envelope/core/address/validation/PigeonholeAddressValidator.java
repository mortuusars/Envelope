package io.github.mortuusars.envelope.core.address.validation;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.core.address.Address;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Optional;

public class PigeonholeAddressValidator extends AddressValidator {
    public static final Validator.Issue NOT_ENOUGH_XP_LEVELS = () -> "address.not_enough_xp_levels";

    protected final Player player;
    protected int xpLevelsCost = Config.Server.Pigeonhole.ADDRESS_EXPERIENCE_LEVELS_COST.get();
    protected Optional<Address> existingAddress = Optional.empty();

    public PigeonholeAddressValidator(Player player, Optional<Address> existingAddress) {
        this.player = player;
        this.existingAddress = existingAddress;
    }

    public PigeonholeAddressValidator(Player player) {
        this.player = player;
    }

    public int getXpLevelsCost() {
        return xpLevelsCost;
    }

    public PigeonholeAddressValidator setXpLevelsCost(int xpLevelsCost) {
        this.xpLevelsCost = xpLevelsCost;
        return this;
    }

    public Optional<Address> getExistingAddress() {
        return existingAddress;
    }

    public PigeonholeAddressValidator setExistingAddress(Optional<Address> existingAddress) {
        this.existingAddress = existingAddress;
        return this;
    }

    protected boolean matchesExistingAddress(String addressId) {
        return getExistingAddress().map(address -> address.matches(addressId)).orElse(false);
    }

    // --

    @Override
    public ArrayList<Validator.Issue> validate(String addressId) {
        if (matchesExistingAddress(addressId)) {
            return new ArrayList<>();
        }

        ArrayList<Validator.Issue> issues = super.validate(addressId);

        if (player.experienceLevel < xpLevelsCost) {
            issues.add(NOT_ENOUGH_XP_LEVELS);
        }

        return issues;
    }
}
