package io.github.mortuusars.envelope.util;

/**
 * List of symbols and icons added by Envelope.<br>
 * Added with 'assets/minecraft/font/include/default.json'.<br><br>
 * Utilizing "private-use" range on unicode to avoid overriding existing symbols.
 * It's possible that other mods/resourcepacks could use same symbols and I think it would cause conflicts,
 * but chances of this should be pretty small. And to reduce it further I start at 'uEE00' instead of 'uE000'.
 */
public class EnvelopeSymbols {
    public static final String SMALL_SPACE = "\uEEFF";
    public static final String ADDRESS_GENERIC = "\uEE00";
    public static final String ADDRESS_BLOCK = "\uEE01";
    public static final String ADDRESS_PLAYER = "\uEE02";
    public static final String ADDRESS_SERVICE = "\uEE03";
    public static final String ADDRESS_CUSTOM = "\uEE04";
    public static final String ADDRESS_MAIL_SERVICE = "\uEE05";
    public static final String ADDRESS_UNKNOWN = "\uEE06";
    public static final String LETTER = "\uEE10";
    public static final String SMALL_FILLED_ARROW_LEFT = "\uEE20";
    public static final String SMALL_FILLED_ARROW_RIGHT = "\uEE21";
    public static final String COAT_OF_ARMS = "\uEE30";
}
