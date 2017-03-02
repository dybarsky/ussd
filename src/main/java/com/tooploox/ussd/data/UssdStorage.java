package com.tooploox.ussd.data;

import com.tooploox.ussd.domain.Ussd;

import java.util.ArrayList;
import java.util.List;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 18:09
 */
public class UssdStorage implements UssdRepository {

    private ArrayList<Ussd> list = new ArrayList<>();

    @Override
    public void addUssd(Ussd ussd) {
        list.add(ussd);
    }

    @Override
    public void removeUssd(Ussd ussd) {
        list.remove(ussd);
    }

    @Override
    public void updateUssd(Ussd ussd) {
        for (Ussd u : list) {
            if (u.equals(ussd)) {
                u.setCode(ussd.getCode());
                u.setRegex(ussd.getRegex());
                u.setResponse(ussd.getResponse());
                u.setResult(ussd.getResult());
            }
        }
    }

    @Override
    public List<Ussd> getUssdList() {
        return (List<Ussd>) list.clone();
    }
}
