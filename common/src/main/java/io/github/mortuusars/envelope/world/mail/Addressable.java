package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.core.address.Address;

public interface Addressable<T extends Address> {
    T getAddress();
}
