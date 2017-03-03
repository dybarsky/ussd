package com.tooploox.ussd.data;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;

import com.tooploox.ussd.App;
import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.domain.UssdResultMatcher;

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

    @Override
    public void run(Ussd ussd) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        pendingUssd = ussd;
        String ussdCode = ussd.getCode().replaceAll("#", ENCODED_HASH);
        Uri uri = Uri.parse("tel:" + ussdCode);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        context.startActivity(intent);
    }

    @Override
    public void setResult(String result) {
        pendingUssd.setResponse(result);
        UssdResultMatcher.matchResult(pendingUssd);
        App.INSTANCE.ussdStorage.updateUssd(pendingUssd);
        pendingUssd = null;
    }

}
