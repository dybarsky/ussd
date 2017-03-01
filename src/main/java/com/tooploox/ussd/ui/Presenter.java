package com.tooploox.ussd.ui;

import com.tooploox.ussd.data.UssdStorage;
import com.tooploox.ussd.domain.Ussd;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 17:49
 */
public class Presenter {

    private UssdStorage storage;
    private UssdListAdapter adapter;

    public Presenter(UssdStorage storage, UssdListAdapter adapter) {
        this.storage = storage;
        this.adapter = adapter;
    }

    public void addUssd(Ussd ussd) {
        storage.addUssd(ussd);
        loadUssdList();
    }
    
    public void loadUssdList() {
        adapter.setDataAndInvalidate(storage.getUssdList());
    }
    
    public void runUssd(Ussd ussd) {

        
    }
}
