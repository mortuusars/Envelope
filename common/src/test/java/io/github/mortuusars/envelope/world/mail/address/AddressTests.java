package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddressTests {
    @Test
    void addressesAreValid() {
        assertThrows(IllegalStateException.class, () -> new BlockAddress(""));
        assertThrows(IllegalStateException.class, () -> new BlockAddress(" "));
        assertThrows(IllegalStateException.class, () -> new BlockAddress("too-long".repeat(30)));

        assertThrows(IllegalStateException.class, () -> new PlayerAddress(""));
        assertThrows(IllegalStateException.class, () -> new PlayerAddress(" "));
        assertThrows(IllegalStateException.class, () -> new PlayerAddress("too-long".repeat(30)));
    }

    @Test
    void equality() {
        assertEquals(new BlockAddress("Id"), new BlockAddress("Id"));
        assertEquals(new BlockAddress("Case Insensitive"), new BlockAddress("caSE iNSeNSitiVe"));
        assertNotEquals(new BlockAddress("Case Insensitive"), new BlockAddress("Different caSE iNSeNSitiVe"));

        assertEquals(new PlayerAddress("Id"), new PlayerAddress("Id"));
        assertEquals(new PlayerAddress("Case Insensitive"), new PlayerAddress("caSE iNSeNSitiVe"));
        assertNotEquals(new PlayerAddress("Case Insensitive"), new PlayerAddress("Different caSE iNSeNSitiVe"));
    }

    @Test
    void matching() {
//        assertTrue(new BlockAddress("Id").matches(new BlockAddress("Id")));
//        assertTrue(new BlockAddress("Case Insensitive").matches(new BlockAddress("caSE iNSeNSitiVe")));
//        assertFalse(new BlockAddress("Id").matches(new BlockAddress("Different Id")));
//
//        assertTrue(new PlayerAddress("Id").matches(new PlayerAddress("Id")));
//        assertTrue(new PlayerAddress("Case Insensitive").matches(new PlayerAddress("caSE iNSeNSitiVe")));
//        assertFalse(new PlayerAddress("Id").matches(new PlayerAddress("Different Id")));
//
//        assertTrue(new EntityAddress("Id").matches(new EntityAddress("Id")));
//        assertTrue(new EntityAddress("Case Insensitive").matches(new EntityAddress("caSE iNSeNSitiVe")));
//        assertFalse(new EntityAddress("Id").matches(new EntityAddress("Different Id")));
//
//        assertTrue(new BlockAddress("Id").matches(new EntityAddress("Id")));
//        assertTrue(new BlockAddress("Villager").matches(new EntityAddress("Id", Component.literal("Villager"))));
//        assertFalse(new BlockAddress("Villager").matches(new EntityAddress("Id", Component.literal("Different Villager"))));
//        assertFalse(new EntityAddress("Villager").matches(new EntityAddress("Id", Component.literal("Different Villager"))));
    }

    @Test
    void hashCodes() {
        assertEquals(new BlockAddress("Id").hashCode(), new BlockAddress("Id").hashCode());
        assertEquals(new BlockAddress("Case Insensitive").hashCode(), new BlockAddress("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new BlockAddress("Case Insensitive").hashCode(), new BlockAddress("Different caSE iNSeNSitiVe").hashCode());

        assertEquals(new PlayerAddress("Id").hashCode(), new PlayerAddress("Id").hashCode());
        assertEquals(new PlayerAddress("Case Insensitive").hashCode(), new PlayerAddress("caSE iNSeNSitiVe").hashCode());
        assertNotEquals(new PlayerAddress("Case Insensitive").hashCode(), new PlayerAddress("Different caSE iNSeNSitiVe").hashCode());

//        assertEquals(new EntityAddress("Id").hashCode(), new EntityAddress("Id").hashCode());
//        assertEquals(new EntityAddress("Case Insensitive").hashCode(), new EntityAddress("caSE iNSeNSitiVe").hashCode());
//        assertNotEquals(new EntityAddress("Case Insensitive").hashCode(), new EntityAddress("Different caSE iNSeNSitiVe").hashCode());
    }

    @Test
    void hashCodesOfSameIdButDifferentTypeAreDifferent() {
        assertNotEquals(new BlockAddress("Id").hashCode(), new PlayerAddress("Id").hashCode());
//        assertNotEquals(new BlockAddress("Id").hashCode(), new EntityAddress("Id").hashCode());
        assertNotEquals(new PlayerAddress("Id").hashCode(), new BlockAddress("Id").hashCode());
//        assertNotEquals(new PlayerAddress("Id").hashCode(), new EntityAddress("Id").hashCode());
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