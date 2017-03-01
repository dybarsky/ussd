package com.tooploox.ussd.utils;

import android.support.annotation.Nullable;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:24
 */
public class Strings {

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.length() == 0;
    }
}
