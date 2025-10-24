package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AddressDisplay {
    public static final int GENERIC_COLOR = 0xFFD2BCA4;
    public static final int SENDER_COLOR = 0xFFA3A7D4;
    public static final int RECIPIENT_COLOR = 0xFF93C48F;

    public static final Style GENERIC_ICON_STYLE = Style.EMPTY.withColor(GENERIC_COLOR);
    public static final Style SENDER_ICON_STYLE = Style.EMPTY.withColor(SENDER_COLOR);
    public static final Style RECIPIENT_ICON_STYLE = Style.EMPTY.withColor(RECIPIENT_COLOR);

    public static final Style REGULAR_TEXT_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);

    public static final Component SENDER_TO_RECIPIENT_ARROW = Component.literal(
            EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT + " ").withStyle(REGULAR_TEXT_STYLE);

    public static MutableComponent create(Address address, Style iconStyle, Style textStyle) {
        return Component.literal(getIcon(address)).withStyle(iconStyle)
                .append(EnvelopeSymbols.SMALL_SPACE)
                .append(address.getDisplayName().withStyle(textStyle));
    }

    public static MutableComponent createGeneric(Address address) {
        return create(address, GENERIC_ICON_STYLE, REGULAR_TEXT_STYLE);
    }

    public static @Nullable MutableComponent createSenderToRecipient(@Nullable Address sender, @Nullable Address recipient, Style textStyle) {
        if (sender == null && recipient == null) return null;

        if (sender != null) {
            if (recipient == null) {
                recipient = Address.UNKNOWN;
            }

            return create(sender, SENDER_ICON_STYLE, textStyle)
                    .append(" " + EnvelopeSymbols.SMALL_SPACE)
                    .append(SENDER_TO_RECIPIENT_ARROW)
                    .append(create(recipient, RECIPIENT_ICON_STYLE, textStyle));
        }

        return Component.empty()
                .append(SENDER_TO_RECIPIENT_ARROW)
                .append(create(recipient, RECIPIENT_ICON_STYLE, textStyle));
    }

    public static @Nullable MutableComponent createSenderToRecipient(@Nullable Address sender, @Nullable Address recipient) {
        return createSenderToRecipient(sender, recipient, REGULAR_TEXT_STYLE);
    }

    public static @NotNull String getIcon(Address address) {
        if (address.equals(Address.UNKNOWN)) return EnvelopeSymbols.ADDRESS_UNKNOWN;
        return switch (address.type()) {
            case PIGEONHOLE -> EnvelopeSymbols.ADDRESS_PIGEONHOLE;
            case PLAYER -> EnvelopeSymbols.ADDRESS_PLAYER;
            case ENTITY -> EnvelopeSymbols.ADDRESS_NPC;
        };
    }
}
