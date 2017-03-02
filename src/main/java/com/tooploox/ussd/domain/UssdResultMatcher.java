package com.tooploox.ussd.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 17:29
 */
public class UssdResultMatcher {

    private static final String defaultResult = "?";

    public static void matchResult(Ussd ussd) {
        Matcher matcher = Pattern
                .compile(ussd.getRegex())
                .matcher(ussd.getResponse());
        String result = matcher.find()
                ? matcher.group()
                : defaultResult;
        ussd.setResult(result);
    }
}
