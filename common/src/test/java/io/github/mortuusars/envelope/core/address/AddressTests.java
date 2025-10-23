package io.github.mortuusars.envelope.core.address;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddressTests {
    @Test
    void equality() {
        assertEquals(new Address.Pigeonhole("Id"), new Address.Pigeonhole("Id"));
        assertEquals(new Address.Pigeonhole("Case Insensitive"), new Address.Pigeonhole("caSE iNSeNSitiVe"));
        assertNotEquals(new Address.Pigeonhole("Case Insensitive"), new Address.Pigeonhole("Different caSE iNSeNSitiVe"));

        assertEquals(new Address.Player("Id"), new Address.Player("Id"));
        assertEquals(new Address.Player("Case Insensitive"), new Address.Player("caSE iNSeNSitiVe"));
        assertNotEquals(new Address.Player("Case Insensitive"), new Address.Player("Different caSE iNSeNSitiVe"));

        assertEquals(new Address.Entity("Id"), new Address.Entity("Id"));
        assertEquals(new Address.Entity("Case Insensitive"), new Address.Entity("caSE iNSeNSitiVe"));
        assertNotEquals(new Address.Entity("Case Insensitive"), new Address.Entity("Different caSE iNSeNSitiVe"));
    }

    @Test
    void hashCodes() {
        assertEquals(new Address.Pigeonhole("Id").hashCode(), new Address.Pigeonhole("Id").hashCode());
        assertEquals(new Address.Pigeonhole("Case Insensitive").hashCode(), new Address.Pigeonhole("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new Address.Pigeonhole("Case Insensitive").hashCode(), new Address.Pigeonhole("Different caSE iNSeNSitiVe").hashCode());

        assertEquals(new Address.Player("Id").hashCode(), new Address.Player("Id").hashCode());
        assertEquals(new Address.Player("Case Insensitive").hashCode(), new Address.Player("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new Address.Player("Case Insensitive").hashCode(), new Address.Player("Different caSE iNSeNSitiVe").hashCode());

        assertEquals(new Address.Entity("Id").hashCode(), new Address.Entity("Id").hashCode());
        assertEquals(new Address.Entity("Case Insensitive").hashCode(), new Address.Entity("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new Address.Entity("Case Insensitive").hashCode(), new Address.Entity("Different caSE iNSeNSitiVe").hashCode());
    }

    @Test
    void hashCodesOfSameIdButDifferentTypeAreDifferent() {
        assertNotEquals(new Address.Pigeonhole("Id").hashCode(), new Address.Player("Id").hashCode());
        assertNotEquals(new Address.Pigeonhole("Id").hashCode(), new Address.Entity("Id").hashCode());
        assertNotEquals(new Address.Player("Id").hashCode(), new Address.Pigeonhole("Id").hashCode());
        assertNotEquals(new Address.Player("Id").hashCode(), new Address.Entity("Id").hashCode());
    }

    @Test
    void addressWorksAsMapKey() {
        Map<Address, Integer> map = Map.of(
                new Address.Pigeonhole("dev"), 42,
                new Address.Entity("villager"), 23,
                new Address.Player("villager"), 64,
                new Address.Player("dev"), 11
        );

        assertEquals(map.get(new Address.Player("villager")), 64);
        assertEquals(map.get(new Address.Entity("villager")), 23);
        assertEquals(map.get(new Address.Pigeonhole("dev")), 42);
    }
}