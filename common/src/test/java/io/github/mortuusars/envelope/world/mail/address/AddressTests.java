package io.github.mortuusars.envelope.world.mail.address;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddressTests {
    @Test
    void addressesAreValid() {
        assertThrows(IllegalStateException.class, () -> new Address.Block(""));
        assertThrows(IllegalStateException.class, () -> new Address.Block(" "));
        assertThrows(IllegalStateException.class, () -> new Address.Block("too-long".repeat(30)));

        assertThrows(IllegalStateException.class, () -> new Address.Player(""));
        assertThrows(IllegalStateException.class, () -> new Address.Player(" "));
        assertThrows(IllegalStateException.class, () -> new Address.Player("too-long".repeat(30)));

        assertThrows(IllegalStateException.class, () -> new Address.Entity(""));
        assertThrows(IllegalStateException.class, () -> new Address.Entity(" "));
        assertThrows(IllegalStateException.class, () -> new Address.Entity("too-long".repeat(30)));
    }

    @Test
    void equality() {
        assertEquals(new Address.Block("Id"), new Address.Block("Id"));
        assertEquals(new Address.Block("Case Insensitive"), new Address.Block("caSE iNSeNSitiVe"));
        assertNotEquals(new Address.Block("Case Insensitive"), new Address.Block("Different caSE iNSeNSitiVe"));

        assertEquals(new Address.Player("Id"), new Address.Player("Id"));
        assertEquals(new Address.Player("Case Insensitive"), new Address.Player("caSE iNSeNSitiVe"));
        assertNotEquals(new Address.Player("Case Insensitive"), new Address.Player("Different caSE iNSeNSitiVe"));

        assertEquals(new Address.Entity("Id"), new Address.Entity("Id"));
        assertEquals(new Address.Entity("Case Insensitive"), new Address.Entity("caSE iNSeNSitiVe"));
        assertNotEquals(new Address.Entity("Case Insensitive"), new Address.Entity("Different caSE iNSeNSitiVe"));
    }

    @Test
    void hashCodes() {
        assertEquals(new Address.Block("Id").hashCode(), new Address.Block("Id").hashCode());
        assertEquals(new Address.Block("Case Insensitive").hashCode(), new Address.Block("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new Address.Block("Case Insensitive").hashCode(), new Address.Block("Different caSE iNSeNSitiVe").hashCode());

        assertEquals(new Address.Player("Id").hashCode(), new Address.Player("Id").hashCode());
        assertEquals(new Address.Player("Case Insensitive").hashCode(), new Address.Player("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new Address.Player("Case Insensitive").hashCode(), new Address.Player("Different caSE iNSeNSitiVe").hashCode());

        assertEquals(new Address.Entity("Id").hashCode(), new Address.Entity("Id").hashCode());
        assertEquals(new Address.Entity("Case Insensitive").hashCode(), new Address.Entity("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new Address.Entity("Case Insensitive").hashCode(), new Address.Entity("Different caSE iNSeNSitiVe").hashCode());
    }

    @Test
    void hashCodesOfSameIdButDifferentTypeAreDifferent() {
        assertNotEquals(new Address.Block("Id").hashCode(), new Address.Player("Id").hashCode());
        assertNotEquals(new Address.Block("Id").hashCode(), new Address.Entity("Id").hashCode());
        assertNotEquals(new Address.Player("Id").hashCode(), new Address.Block("Id").hashCode());
        assertNotEquals(new Address.Player("Id").hashCode(), new Address.Entity("Id").hashCode());
    }

    @Test
    void addressWorksAsMapKey() {
        Map<Address, Integer> map = Map.of(
                new Address.Block("dev"), 42,
                new Address.Entity("villager"), 23,
                new Address.Player("villager"), 64,
                new Address.Player("dev"), 11
        );

        assertEquals(map.get(new Address.Player("villager")), 64);
        assertEquals(map.get(new Address.Entity("villager")), 23);
        assertEquals(map.get(new Address.Block("dev")), 42);
    }
}