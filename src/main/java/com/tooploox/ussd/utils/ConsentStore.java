package com.tooploox.ussd.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers the user's answers to the onboarding wizard: whether it was completed and whether the user
 * agreed to the prominent disclosure about the AccessibilityService (Google Play User Data policy).
 *
 * Stored in its own preferences file, not in "SimpleStorage" which holds the serialized Ussd entries.
 */
public class ConsentStore {

    private static final String PREFERENCES_NAME = "settings";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_ACCESSIBILITY_ACCEPTED = "accessibility_disclosure_accepted";
    private static final String KEY_ACCESSIBILITY_ACCEPTED_AT = "accessibility_disclosure_accepted_at";

    private final SharedPreferences preferences;

    public ConsentStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingDone() {
        return preferences.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        preferences.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    /** True only after the user tapped "Agree" on the accessibility disclosure. */
    public boolean isAccessibilityAccepted() {
        return preferences.getBoolean(KEY_ACCESSIBILITY_ACCEPTED, false);
    }

    public void setAccessibilityAccepted(boolean accepted) {
        preferences.edit()
                .putBoolean(KEY_ACCESSIBILITY_ACCEPTED, accepted)
                .putLong(KEY_ACCESSIBILITY_ACCEPTED_AT, accepted ? System.currentTimeMillis() : 0L)
                .apply();
    }
}
