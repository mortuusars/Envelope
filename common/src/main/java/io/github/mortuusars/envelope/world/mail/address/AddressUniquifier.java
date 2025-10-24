package io.github.mortuusars.envelope.world.mail.address;

import com.google.common.base.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressUniquifier {
    private static final Pattern VERSION_PATTERN = Pattern.compile("-(\\d+)$");

    protected final AllAddresses knownAddresses;
    protected final int maxLength;

    public AddressUniquifier(AllAddresses knownAddresses, int maxLength) {
        Preconditions.checkArgument(maxLength > 2);
        this.knownAddresses = knownAddresses;
        this.maxLength = maxLength;
    }

    public String uniquify(String address) {
        if (!knownAddresses.isKnown(address)) {
            return address;
        }

        int number = 1;
        Matcher matcher = VERSION_PATTERN.matcher(address);
        if (matcher.find()) {
            try {
                number = Integer.parseInt(matcher.group(1));
                address = address.substring(0, matcher.start());
            } catch (NumberFormatException ignored) { }
        }

        String candidate = address + "-" + number;
        while (knownAddresses.isKnown(candidate)) {
            number++;
            candidate = address + "-" + number;
        }

        if (candidate.length() > maxLength) {
            String suffix = "-" + number;
            candidate = address.substring(0, maxLength - suffix.length()) + suffix;
        }

        return candidate;
    }
}
