package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.validation.Issue;
import io.github.mortuusars.envelope.util.validation.Rule;
import io.github.mortuusars.envelope.util.validation.Validator;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public abstract class AddressValidation {
    public static Issue CANNOT_BE_EMPTY = () -> "address.empty";
    public static Issue TOO_LONG = () -> "address.too_long";
    public static Issue CONTAINS_INVALID_CHARS = () -> "address.contains_invalid_chars";
    public static Issue TAKEN = () -> "address.taken";
    public static Issue NOT_ENOUGH_XP = () -> "address.not_enough_xp_levels";

    public static final int MAX_LENGTH = 22;

    private static final Validator<String> FORMAT = Validator.of(
          Rule.when(String::isBlank, CANNOT_BE_EMPTY),
          Rule.when(id -> id.length() > MAX_LENGTH, TOO_LONG),
          Rule.when(id -> !StringUtil.filterText(id).equals(id), CONTAINS_INVALID_CHARS)
    );

    public static Validator<String> format() {
        return FORMAT;
    }

    public static Validator<String> forPigeonhole(Supplier<AllAddresses> addresses, Supplier<Player> player) {
        return format()
              .and(isNotTaken(addresses))
              .and(hasEnoughXp(player, Config.Server.PIGEONHOLE_ADDRESS_EXPERIENCE_LEVELS_COST.get()));
    }

    // --

    public static Rule<String> isNotTaken(Supplier<AllAddresses> addresses) {
        return Rule.when(id -> addresses.get().isKnown(id), TAKEN);
    }

    public static Rule<String> hasEnoughXp(Supplier<Player> player, int xpLevelsRequired) {
        return Rule.when(id -> {
            Player pl = player.get();
            return !pl.isCreative() && pl.experienceLevel < xpLevelsRequired;
        }, NOT_ENOUGH_XP);
    }
}
