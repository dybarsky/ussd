package com.tooploox.ussd;

import android.app.Application;

import com.tooploox.ussd.data.UssdExecutor;
import com.tooploox.ussd.data.UssdRepository;
import com.tooploox.ussd.data.UssdRunner;
import com.tooploox.ussd.data.UssdStorage;
import com.tooploox.ussd.utils.EventBus;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 16:56
 */
public class App extends Application {

    public static App INSTANCE;

    public final UssdRepository ussdStorage = new UssdStorage();
    public final UssdExecutor ussdExecutor = new UssdRunner(this);
    public final EventBus eventBus = new EventBus(this);

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }
}
