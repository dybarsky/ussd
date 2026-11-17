package com.tooploox.ussd.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.ui.Serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 18:09
 */
public class UssdSimpleStorage implements UssdRepository {

    private static final String PREFERENCES_NAME = "SimpleStorage";
    /**
     * Stable order for the list: code (case-insensitive, nulls last), then id. SharedPreferences.getAll() has no
     * defined order and the model has no creation timestamp.
     */
    static final Comparator<Ussd> ORDER = new Comparator<Ussd>() {
        @Override
        public int compare(Ussd a, Ussd b) {
            int result = compareNullsLast(a.getCode(), b.getCode(), String.CASE_INSENSITIVE_ORDER);
            if (result != 0) {
                return result;
            }
            return compareNullsLast(a.getId(), b.getId(), null);
        }
    };

    private static int compareNullsLast(String a, String b, Comparator<String> comparator) {
        if (a == null || b == null) {
            return a == b ? 0 : (a == null ? 1 : -1);
        }
        return comparator != null ? comparator.compare(a, b) : a.compareTo(b);
    }

    private Context context;

    public UssdSimpleStorage(Context context) {
        this.context = context;
    }

    @Override
    public void addUssd(Ussd ussd) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .putString(ussd.getId(), Serializer.serialize(ussd))
                .apply();
    }

    @Override
    public void removeUssd(Ussd ussd) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .remove(ussd.getId())
                .apply();
    }

    @Override
    public void updateUssd(Ussd ussd) {
        addUssd(ussd);
    }

    @Override
    public List<Ussd> getUssdList() {
        List<Ussd> ussdList = new ArrayList<>();
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        Map<String, ?> serializedUssdMap = preferences.getAll();
        for (Map.Entry<String, ?> entry : serializedUssdMap.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String)) {
                continue;
            }
            try {
                Ussd ussd = Serializer.deserialize((String) value);
                if (ussd != null) {
                    ussdList.add(ussd);
                }
            } catch (RuntimeException e) {
                // Unreadable entry (bad Base64, wrong type, ...): skip it instead of failing the whole list.
                e.printStackTrace();
            }
        }
        Collections.sort(ussdList, ORDER);
        return ussdList;
    }
}
