package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class AddressFormatter {
    public static final int NEUTRAL_COLOR = 0xFFE6CB98;
    public static final int SENDER_COLOR = 0xFF7AB5E1;
    public static final int RECIPIENT_COLOR = 0xFFE8AD77;

    protected final Address.Realized address;
    protected boolean icon = false;
    protected String iconSeparator = EnvelopeSymbols.SMALL_SPACE;
    protected Either<ChatFormatting, Style> iconStyle = Either.right(Style.EMPTY);
    protected Either<ChatFormatting, Style> textStyle = Either.right(Style.EMPTY);
    protected int maxLength = Integer.MAX_VALUE;

    protected AddressFormatter(Address.Realized address) {
        this.address = address;
    }

    public static AddressFormatter of(Address.Realized address) {
        return new AddressFormatter(address);
    }

    // --

    public AddressFormatter asNeutral() {
        return this
              .withIcon()
              .withIconColor(NEUTRAL_COLOR)
              .withColor(NEUTRAL_COLOR);
    }

    public AddressFormatter asSender() {
        return this
              .withIcon()
              .withIconColor(SENDER_COLOR)
              .withColor(SENDER_COLOR);
    }

    public AddressFormatter asRecipient() {
        return this
              .withIcon()
              .withIconColor(RECIPIENT_COLOR)
              .withColor(RECIPIENT_COLOR);
    }

    // --

    public AddressFormatter withIcon() {
        this.icon = true;
        return this;
    }

    public AddressFormatter withIconSeparator(String separator) {
        this.iconSeparator = separator;
        return this;
    }

    public AddressFormatter withIconColor(ChatFormatting formatting) {
        this.iconStyle = Either.left(formatting);
        return this;
    }

    public AddressFormatter withIconStyle(Style style) {
        this.iconStyle = Either.right(style);
        return this;
    }

    public AddressFormatter withIconColor(int color) {
        this.iconStyle = Either.right(Style.EMPTY.withColor(color));
        return this;
    }

    public AddressFormatter withColor(ChatFormatting formatting) {
        this.textStyle = Either.left(formatting);
        return this;
    }

    public AddressFormatter withStyle(Style style) {
        this.textStyle = Either.right(style);
        return this;
    }

    public AddressFormatter withColor(int color) {
        this.textStyle = Either.right(Style.EMPTY.withColor(color));
        return this;
    }

    public AddressFormatter withMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    // --

    public MutableComponent toComponent() {
        MutableComponent addressComponent;
        if (maxLength < Integer.MAX_VALUE) {
            String string = StringUtil.truncateStringIfNecessary(address.toString(), maxLength, true);
            addressComponent = Component.literal(string);
        } else {
            addressComponent = address.getDisplayComponent();
        }
        addressComponent = addressComponent.withStyle(createStyle(textStyle));

        if (icon) {
            return Component.empty()
                  .append(Component.literal(getIcon(address)).withStyle(createStyle(iconStyle)))
                  .append(iconSeparator)
                  .append(addressComponent);
        }

        return addressComponent;
    }

    @Override
    public String toString() {
        String addressString = StringUtil.truncateStringIfNecessary(address.toString(), maxLength, true);

        String addressText = textStyle.left()
              .map(formatting -> formatting + addressString + ChatFormatting.RESET)
              .orElse(addressString);

        if (icon) {
            String icon = iconStyle.left()
                  .map(formatting -> formatting + getIcon(address) + ChatFormatting.RESET)
                  .orElse(getIcon(address));
            return icon + iconSeparator + addressText;
        }

        return addressText;
    }

    protected Style createStyle(Either<ChatFormatting, Style> style) {
        return style.map(Style.EMPTY::applyFormat, Function.identity());
    }

    // --

    public static @NotNull String getIcon(Address address) {
        return switch (address.getType()) {
            case BLOCK -> EnvelopeSymbols.ADDRESS_BLOCK;
            case PLAYER -> EnvelopeSymbols.ADDRESS_PLAYER;
            case ENTITY -> address.isMailService() ? EnvelopeSymbols.ADDRESS_MAIL_SERVICE : EnvelopeSymbols.ADDRESS_ENTITY;
            case CUSTOM -> EnvelopeSymbols.ADDRESS_CUSTOM;
            case UNKNOWN -> EnvelopeSymbols.ADDRESS_UNKNOWN;
        };
    }

//    public static @NotNull Component fullSender(@Nullable Address sender) {
//        if (sender == null) return CommonComponents.EMPTY;
//        return Component.translatable("gui.envelope.mail.sender").withStyle(ChatFormatting.GRAY)
//              .append(": ").withStyle(ChatFormatting.GRAY)
//              .append(sender.format().asSender().toComponent());
//    }
//
//    public static @NotNull Component fullRecipient(@Nullable Address recipient) {
//        if (recipient == null) return CommonComponents.EMPTY;
//        return Component.translatable("gui.envelope.mail.recipient").withStyle(ChatFormatting.GRAY)
//              .append(": ").withStyle(ChatFormatting.GRAY)
//              .append(recipient.format().asRecipient().toComponent());
//    }

//    public static @NotNull Component senderToRecipient(@Nullable Address sender, @Nullable Address recipient) {
//        if (sender == null && recipient == null) return CommonComponents.EMPTY;
//
//        if (sender != null) {
//            if (recipient == null) {
//                recipient = Address.UNKNOWN;
//            }
//
//            return sender.format().asSender().withMaxLength(25).toComponent()
//                  .append(Component.literal(" " + EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT + " ").withStyle(ChatFormatting.GRAY))
//                  .append(recipient.format().asRecipient().withMaxLength(25).toComponent());
//        }
//
//        return recipient.format().asRecipient().toComponent();
//    }
}
