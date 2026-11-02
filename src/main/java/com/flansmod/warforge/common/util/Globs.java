package com.flansmod.warforge.common.util;

import java.util.regex.Pattern;

public final class Globs {
    private Globs() {}

    /** {@code *} matches any run of characters; everything else is literal. */
    public static boolean matches(String pattern, String value) {
        return value.matches(Pattern.quote(pattern).replace("*", "\\E.*\\Q"));
    }
}
