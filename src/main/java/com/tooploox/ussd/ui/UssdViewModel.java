package com.tooploox.ussd.ui;

import com.tooploox.ussd.domain.Ussd;

import java.util.List;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:29
 */
public interface UssdViewModel {

    void setDataAndInvalidateView(List<Ussd> data);
}
