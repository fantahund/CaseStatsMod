package de.fanta.casestats.globaldata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtilCore {
    public static final ToIntFunction<String> CASE_IGNORING_HASHER = s -> {
        if (s == null) {
            return 0;
        }

        int hash = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            hash = 31 * hash + Character.toLowerCase(c);
        }
        return hash;
    };

    public static final BiPredicate<String, String> CASE_IGNORING_EQUALITY = (s1, s2) -> s1 == null ? s2 == null : s1.equalsIgnoreCase(s2);

    /**
     * Filters all incomplete surrogates from a string while keeping all other chars.
     * If no incomplete surrogates are found the input string is returned.
     *
     * @param input
     *            a string
     * @return a variant of the input string where all incomplete surrogates are removed.
     */
    public static String filterIncompleteSurrogatePairs(String input) {
        StringBuilder sb = null;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
                // no surrogate, so always valid
                if (sb != null) {
                    sb.append(c);
                }
            } else {
                if (c >= Character.MIN_HIGH_SURROGATE && c <= Character.MAX_HIGH_SURROGATE && i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    if (next >= Character.MIN_LOW_SURROGATE && next <= Character.MAX_LOW_SURROGATE) {
                        i++;
                        if (Character.isDefined(Character.toCodePoint(c, next))) {
                            // valid surrogate pair
                            if (sb != null) {
                                sb.append(c);
                                sb.append(next);
                            }
                        } else {
                            // undefined character, skip both
                            if (sb == null) {
                                sb = new StringBuilder();
                                sb.append(input, 0, i - 1);
                            }
                        }
                    } else {
                        // missing low surrogate, skip the high surrogate
                        if (sb == null) {
                            sb = new StringBuilder();
                            sb.append(input, 0, i);
                        }
                    }
                } else {
                    // low surrogate without a previous high surrogate or high surrogate at the end of the string, skip
                    if (sb == null) {
                        sb = new StringBuilder();
                        sb.append(input, 0, i);
                    }
                }
            }
        }
        return sb != null ? sb.toString() : input;
    }

    /**
     * Filters all control characters from a string
     *
     * @param input
     *            a string
     * @param allowNewline
     *            allow new lines in the string or filter them
     * @return a variant of the input string where all control characters are removed.
     */
    public static String filterControlCharacters(String input, boolean allowNewline) {
        StringBuilder sb = null;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c < 32 && (!allowNewline || c != '\n')) || (c >= 127 && c < 160)) {
                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(input, 0, i);
                }
            } else {
                if (sb != null) {
                    sb.append(c);
                }
            }
        }
        return sb != null ? sb.toString() : input;
    }

    public static int findMatchingBrace(String s) {
        int open = 1;
        for (int i = 1; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '{' -> open++;
                case '}' -> {
                    if (--open == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static ArrayList<String> copyPartialMatches(String token, Collection<String> unfiltered) {
        return unfiltered.stream().filter(s -> startsWithIgnoreCase(s, token)).collect(Collectors.toCollection(() -> new ArrayList<>()));
    }

    public static boolean startsWithIgnoreCase(final String string, final String prefix) {
        return string.length() >= prefix.length() && string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static final char COLOR_CHAR = '§';
    public static final Pattern COLOR_CHAR_PATTERN = Pattern.compile("\\" + COLOR_CHAR);
    public static final Pattern COLOR_CODES_PATTERN = Pattern.compile("\\" + COLOR_CHAR + "([0-9a-fk-or]|(x(" + COLOR_CHAR + "[0-9a-f]){6}))", Pattern.CASE_INSENSITIVE);

    public static String convertColors(String text) {
        return parseColors(text, false);
    }

    public static String stripColors(String text) {
        return parseColors(text, true);
    }

    private static String parseColors(String text, boolean remove) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = null;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char current = text.charAt(i);
            if (current == '&' && i + 1 < len) {
                char next = text.charAt(i + 1);
                // if next is a "&" skip next char
                // if its a color char replace the "&"
                if (NamedChatColor.getByCode(next) != null || next == '&' || next == 'x') {
                    if (builder == null) {
                        builder = new StringBuilder();
                        builder.append(text, 0, i);
                    }
                    i++;
                    if (next != '&') {
                        if (next == 'x') {
                            Integer hex = parseHexColor(text, i + 1);
                            if (hex == null) {
                                builder.append(current).append(next);
                            } else {
                                if (!remove) {
                                    String hexString = Integer.toString(hex, 16);
                                    builder.append(COLOR_CHAR).append('x');
                                    int offset = hexString.length() - 6;
                                    for (int j = 0; j < 6; j++) {
                                        int charPos = j + offset;
                                        char c = charPos >= 0 ? hexString.charAt(charPos) : '0';
                                        builder.append(COLOR_CHAR).append(c);
                                    }
                                }
                                i += 6;
                            }
                        } else {
                            if (!remove) {
                                builder.append(COLOR_CHAR).append(next);
                            }
                        }
                        continue;
                    }
                }
            }
            if (builder != null) {
                builder.append(current);
            }
        }
        return builder == null ? text : builder.toString();
    }

    public static Integer parseHexColor(String text, int startIndex) {
        if (text.length() - startIndex < 6) {
            return null;
        }
        StringBuilder hexString = new StringBuilder("");
        for (int i = 0; i < 6; i++) {
            char c = Character.toLowerCase(text.charAt(i + startIndex));
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                return null;
            }
            hexString.append(c);
        }
        return Integer.parseInt(hexString.toString(), 16);
    }

    public static String revertColors(String converted) {
        if (converted == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < converted.length(); i++) {
            char c = converted.charAt(i);
            if (c == COLOR_CHAR) {
                if (converted.length() > i + 1 && converted.charAt(i + 1) == 'x') {
                    if (i + 14 > converted.length()) {
                        builder.append("&");
                        continue;
                    }
                    String hexString = converted.substring(i, i + 14);
                    if (!COLOR_CODES_PATTERN.matcher(hexString).matches()) {
                        builder.append("&");
                        continue;
                    }
                    builder.append("&").append(COLOR_CHAR_PATTERN.matcher(hexString).replaceAll(""));
                    i += 13;
                    continue;
                }
                builder.append("&");
            } else if (c == '&') {
                builder.append("&&");
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
