package com.tooploox.ussd.data;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;

import com.tooploox.ussd.domain.Ussd;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:43
 */
public class UssdRunner implements UssdExecutor {

    private static final String encodedHash = Uri.encode("#");
    private Context context;

    public UssdRunner(Context context) {
        this.context = context;
    }

    @Override
    public void run(Ussd ussd) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String ussdCode = ussd.getCode().replaceAll("#", encodedHash);
        Uri uri = Uri.parse("tel:" + ussdCode);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        context.startActivity(intent);
    }
}
