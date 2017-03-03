package com.tooploox.ussd.domain;

import com.tooploox.ussd.utils.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 17:29
 */
public class UssdResultMatcher {

    private static final String DEFAULT_RESULT = "?";

    public static void matchResult(Ussd ussd) {
        String result = DEFAULT_RESULT;
        if (!Strings.isNullOrEmpty(ussd.getResponse())) {
            Matcher matcher = Pattern
                    .compile(ussd.getRegex())
                    .matcher(ussd.getResponse());
            result = matcher.find()
                    ? matcher.group()
                    : DEFAULT_RESULT;
        }
        ussd.setResult(result);
    }
}
