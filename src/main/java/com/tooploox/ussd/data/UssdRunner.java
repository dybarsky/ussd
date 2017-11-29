package com.tooploox.ussd.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.tooploox.ussd.App;
import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.utils.SetupHelper;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:43
 */
public class UssdRunner implements UssdExecutor {

    private static final String ENCODED_HASH = Uri.encode("#");

    private Context context;
    private Ussd pendingUssd;

    public UssdRunner(Context context) {
        this.context = context;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run(Ussd ussd) {
        if (!SetupHelper.isPermissionGranted(context)) {
            return;
        }
        pendingUssd = ussd.clone();
        String ussdCode = ussd.getCode().replaceAll("#", ENCODED_HASH);
        Uri uri = Uri.parse("tel:" + ussdCode);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void setResponse(String result) {
        if (pendingUssd != null) {
            pendingUssd.setResponse(result);
            App.INSTANCE.ussdStorage.updateUssd(pendingUssd);
            pendingUssd = null;
        }
    }
}
