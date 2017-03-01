package com.tooploox.ussd.ui;

import com.tooploox.ussd.data.UssdRepository;
import com.tooploox.ussd.domain.Ussd;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 17:49
 */
public class Presenter {

    private UssdRepository repository;
    private UssdViewModel viewModel;

    public Presenter(UssdRepository ussdRepository, UssdViewModel viewModel) {
        this.repository = ussdRepository;
        this.viewModel = viewModel;
    }

    public void addUssd(Ussd ussd) {
        repository.addUssd(ussd);
        loadUssdList();
    }
    
    public void loadUssdList() {
        viewModel.setDataAndInvalidateView(repository.getUssdList());
    }
    
    public void runUssd(Ussd ussd) {

        
    }
}
