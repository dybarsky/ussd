package com.tooploox.ussd.ui;

import com.tooploox.ussd.data.UssdExecutor;
import com.tooploox.ussd.data.UssdRepository;
import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.domain.UssdResultMatcher;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 17:49
 */
public class UssdListPresenter {

    private UssdRepository repository;
    private UssdViewModel viewModel;
    private UssdExecutor executor;

    public UssdListPresenter(UssdRepository ussdRepository, UssdViewModel viewModel, UssdExecutor executor) {
        this.repository = ussdRepository;
        this.viewModel = viewModel;
        this.executor = executor;
    }

    public void addUssd(String code, String regex) {
        Ussd ussd = new Ussd();
        ussd.setCode(code);
        ussd.setRegex(regex);
        UssdResultMatcher.matchResult(ussd); // init default result
        repository.addUssd(ussd);
        loadUssdList();
    }
    
    public void loadUssdList() {
        viewModel.setDataAndInvalidateView(repository.getUssdList());
    }
    
    public void runUssd(Ussd ussd) {
        executor.run(ussd);
    }
}
