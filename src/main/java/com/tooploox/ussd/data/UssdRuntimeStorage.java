package com.tooploox.ussd.data;

import com.tooploox.ussd.domain.Ussd;

import java.util.ArrayList;
import java.util.List;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 18:09
 */
public class UssdRuntimeStorage implements UssdRepository {

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
            if (u.getId().equals(ussd.getId())) {
                u.setCode(ussd.getCode());
                u.setResponse(ussd.getResponse());
            }
        }
    }

    @Override
    public List<Ussd> getUssdList() {
        return (List<Ussd>) list.clone();
    }
}
