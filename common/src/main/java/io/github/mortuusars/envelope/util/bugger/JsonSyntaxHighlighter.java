package io.github.mortuusars.envelope.util.bugger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonSyntaxHighlighter {
    private static final String RESET = "§r";
    private static final String KEYS = "§b";
    private static final String STRINGS = "§a";
    private static final String NUMBERS = "§6";
    private static final String BOOLEANS_AND_NULL = "§c";

    public static String highlight(String json) {
        StringBuilder highlighted = new StringBuilder();

        // Regular expressions for JSON elements
        Pattern pattern = Pattern.compile(
                "(\".*?\"\\s*:)|" +   // Keys
                "(\".*?\")|" +        // Strings
                "(\\b\\d+\\b)|" +     // Numbers
                "(\\btrue\\b|\\bfalse\\b|\\bnull\\b)|" + // Booleans and null
                "([{}\\[\\]])"        // Braces and brackets
        );

        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            if (matcher.group(1) != null) { // Keys
                matcher.appendReplacement(highlighted, KEYS + matcher.group() + RESET);
            } else if (matcher.group(2) != null) { // Strings
                matcher.appendReplacement(highlighted, STRINGS + matcher.group() + RESET);
            } else if (matcher.group(3) != null) { // Numbers
                matcher.appendReplacement(highlighted, NUMBERS + matcher.group() + RESET);
            } else if (matcher.group(4) != null) { // Booleans and null
                matcher.appendReplacement(highlighted, BOOLEANS_AND_NULL + matcher.group() + RESET);
            } else if (matcher.group(5) != null) { // Braces and brackets
                matcher.appendReplacement(highlighted, matcher.group());
            }
        }
        matcher.appendTail(highlighted);
        return highlighted.toString();
    }
}
