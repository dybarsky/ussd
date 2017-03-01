package com.tooploox.ussd.data;

import com.tooploox.ussd.domain.Ussd;

import java.util.List;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:27
 */
public interface UssdRepository {

    void addUssd(Ussd ussd);

    void removeUssd(Ussd ussd);

    List<Ussd> getUssdList();
}
