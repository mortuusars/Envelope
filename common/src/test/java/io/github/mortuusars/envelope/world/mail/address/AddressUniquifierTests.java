package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class AddressUniquifierTests {
    @Test
    void returnsSameWhenAlreadyUnique() {
        assertEquals("Free", uniquify("Free"));
        assertEquals("Free-Twenty-Two-Chars-", uniquify("Free-Twenty-Two-Chars-"));
    }

    @Test
    void returnsUniqueWhenKnownIsPassed() {
        assertEquals("Addr-3", uniquify("Addr"));
        assertEquals("Addr-3", uniquify("Addr-2"));
        assertEquals("Addr-3", uniquify("Addr-3"));
        assertEquals("Addr-6", uniquify("Addr-5"));
        assertEquals("Addr--1", uniquify("Addr-"));
    }

    @Test
    void returnsProperLengthIfOverLimit() {
        assertEquals("Addr-too-long-of-an-address-asdasdaasd-1", uniquify("Addr-too-long-of-an-address-asdasdaasdas"));
        assertEquals("Addr-too-long-of-an-address-asdasdaasd-5", uniquify("Addr-too-long-of-an-address-asdasdaasd-4"));
    }

    // --

    public static String uniquify(String address) {
        return createDefaultUniquifier().uniquify(address);
    }

    public static AddressUniquifier createDefaultUniquifier() {
        AllAddresses addresses = new AllAddresses(
              Set.of("Addr", "Addr-1", "Addr-2", "Addr-5", "Addr-", "Addr----",
                    "Addr-too-long-of-an-address-asdasdaasdas",
                    "Addr-too-long-of-an-address-asdasdaasd-4").stream().map(BlockAddress::new).collect(Collectors.toSet()),
              Set.of("Addrtoo", "Addrtoolon9", "Addrtsdas", "sssdaasd4").stream().map(PlayerAddress::new).collect(Collectors.toSet()),
              Set.of());
        return new AddressUniquifier(addresses);
    }
}
