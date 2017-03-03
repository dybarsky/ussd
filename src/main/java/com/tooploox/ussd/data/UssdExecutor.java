package com.tooploox.ussd.data;

import com.tooploox.ussd.domain.Ussd;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:41
 */
public interface UssdExecutor {

    void run(Ussd ussd);

    void setResponse(String result);
}
