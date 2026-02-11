package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddressTests {
    @Test
    void addressesAreValid() {
        assertThrows(IllegalArgumentException.class, () -> new BlockAddress(""));
        assertThrows(IllegalArgumentException.class, () -> new BlockAddress(" "));
        assertThrows(IllegalArgumentException.class, () -> new BlockAddress("too-long".repeat(30)));

        assertThrows(IllegalArgumentException.class, () -> new PlayerAddress(""));
        assertThrows(IllegalArgumentException.class, () -> new PlayerAddress(" "));
        assertThrows(IllegalArgumentException.class, () -> new PlayerAddress("too-long".repeat(30)));
    }

    @Test
    void equality() {
        assertEquals(new BlockAddress("Id"), new BlockAddress("Id"));
        assertEquals(new BlockAddress("Case Insensitive"), new BlockAddress("caSE iNSeNSitiVe"));
        assertNotEquals(new BlockAddress("Case Insensitive"), new BlockAddress("Different caSE iNSeNSitiVe"));

        assertEquals(new PlayerAddress("Id"), new PlayerAddress("Id"));
        assertEquals(new PlayerAddress("CaseInsensitive"), new PlayerAddress("caSEiNSeNSitiVe"));
        assertNotEquals(new PlayerAddress("CaseInsensitive"), new PlayerAddress("123iNSeNSitiVe"));
    }

    @Test
    void matching() {
        assertTrue(new BlockAddress("Id").matches("Id"));
        assertTrue(new BlockAddress("Case Insensitive").matches("caSE iNSeNSitiVe"));
        assertFalse(new BlockAddress("Id").matches("Different Id"));

        assertTrue(new PlayerAddress("Id").matches("Id"));
        assertTrue(new PlayerAddress("CaseInsensitive").matches("caSEiNSeNSitiVe"));
        assertFalse(new PlayerAddress("Id").matches("Different Id"));
    }

    @Test
    void hashCodes() {
        assertEquals(new BlockAddress("Id").hashCode(), new BlockAddress("Id").hashCode());
        assertEquals(new BlockAddress("Case Insensitive").hashCode(), new BlockAddress("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new BlockAddress("Case Insensitive").hashCode(), new BlockAddress("Different caSE iNSeNSitiVe").hashCode());

        assertEquals(new PlayerAddress("Id").hashCode(), new PlayerAddress("Id").hashCode());
        assertEquals(new PlayerAddress("CaseInsensitive").hashCode(), new PlayerAddress("caSEiNSeNSitiVe").hashCode());
        assertNotEquals(new PlayerAddress("CaseInsensitive").hashCode(), new PlayerAddress("123iNSeNSitiVe").hashCode());
    }

    @Test
    void hashCodesOfSameIdButDifferentTypeAreDifferent() {
        assertNotEquals(new BlockAddress("Id").hashCode(), new PlayerAddress("Id").hashCode());
        assertNotEquals(new PlayerAddress("Id").hashCode(), new BlockAddress("Id").hashCode());
    }

    @Test
    void addressWorksAsMapKey() {
        Map<Address, Integer> map = Map.of(
                new BlockAddress("dev"), 42,
                new PlayerAddress("villager"), 64,
                new PlayerAddress("dev"), 11
        );

        assertEquals(64, map.get(new PlayerAddress("villager")));
        assertEquals(42, map.get(new BlockAddress("dev")));
    }
}