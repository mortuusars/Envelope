package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.util.validation.Rule;
import io.github.mortuusars.envelope.util.validation.Validator;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public interface AddressValidation {
    Error CANNOT_BE_EMPTY = new Error("Id cannot be empty", "error.envelope.address.id_cannot_be_empty");
    Error TOO_LONG = new Error("Id is too long", "error.envelope.address.id_too_long");
    Error CONTAINS_INVALID_CHARS = new Error("Id is too long", "error.envelope.address.id_contains_invalid_chars");
    Error TAKEN = new Error("Id is in use", "error.envelope.address.taken");
    Error NOT_ENOUGH_XP = new Error("Not enough xp levels", "error.envelope.address.not_enough_xp_levels");

    Validator<String> ID = Validator.of(
          Rule.when(StringUtil::isBlank, CANNOT_BE_EMPTY),
          Rule.when(id -> id.length() > Address.MAX_LENGTH, TOO_LONG),
          Rule.when(id -> !StringUtil.filterText(id).equals(id), CONTAINS_INVALID_CHARS)
    );

    static Validator<String> id() {
        return ID;
    }

    static Validator<String> forMailbox(AllAddresses.Realized addresses, Player player) {
        return id()
              .and(isNotTaken(addresses))
              .and(hasEnoughXp(player, Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get()));
    }

    // --

    static Rule<String> isNotTaken(AllAddresses.Realized addresses) {
        return Rule.when(addresses::isKnown, TAKEN);
    }

    static Rule<String> hasEnoughXp(Player player, int xpLevelsRequired) {
        return Rule.when(id -> !player.isCreative() && player.experienceLevel < xpLevelsRequired, NOT_ENOUGH_XP);
    }

    static @NotNull DataResult<String> validateId(String id) {
        return id()
              .test(id)
              .map(DataResult::success, Error::asDataResult);
    }
}
