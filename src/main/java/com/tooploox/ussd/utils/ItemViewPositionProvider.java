package com.tooploox.ussd.utils;

import android.view.View;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 15:23
 */
@FunctionalInterface
public interface ItemViewPositionProvider {

    int getPositon(View view);
}
