package io.github.mortuusars.envelope.core.address.validation;

import io.github.mortuusars.envelope.core.address.AllAddresses;
import net.minecraft.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;

public class AddressValidator implements Validator<String> {
    Issue EMPTY = () -> "address.empty";
    Issue TOO_LONG = () -> "address.too_long";
    Issue CONTAINS_INVALID_CHARS = () -> "address.contains_invalid_chars";
    Issue TAKEN = () -> "address.taken";

    protected int maxLength = 22;
    protected AllAddresses knownAddresses = new AllAddresses(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    public int getMaxLength() {
        return maxLength;
    }

    public AddressValidator setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public AllAddresses getKnownAddresses() {
        return knownAddresses;
    }

    public AddressValidator setKnownAddresses(AllAddresses knownAddresses) {
        this.knownAddresses = knownAddresses;
        return this;
    }

    // --

    public ArrayList<Issue> validate(String addressId) {
        ArrayList<Issue> issues = new ArrayList<>();

        if (addressId.isBlank()) {
            issues.add(EMPTY);
            return issues;
        }

        if (addressId.length() > maxLength) {
            issues.add(TOO_LONG);
        }

        if (!StringUtil.filterText(addressId).equals(addressId)) {
            issues.add(CONTAINS_INVALID_CHARS);
        }

        if (knownAddresses.isKnown(addressId)) {
            issues.add(TAKEN);
        }

        return issues;
    }
}