package com.tooploox.ussd;

import android.app.Application;

import com.tooploox.ussd.data.UssdExecutor;
import com.tooploox.ussd.data.UssdRepository;
import com.tooploox.ussd.data.UssdRunner;
import com.tooploox.ussd.data.UssdSimpleStorage;
import com.tooploox.ussd.utils.EventBus;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 16:56
 */
public class App extends Application {

    public static App INSTANCE;

    public UssdRepository ussdStorage;
    public UssdExecutor ussdExecutor;
    public EventBus eventBus;

    @Override
    public void onCreate() {
        super.onCreate();

        ussdStorage = new UssdSimpleStorage(this);
        ussdExecutor = new UssdRunner(this);
        eventBus = new EventBus(this);

        INSTANCE = this;
    }
}
