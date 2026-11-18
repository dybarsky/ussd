package com.tooploox.ussd.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import com.tooploox.ussd.App;
import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.utils.SetupHelper;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 19:43
 */
public class UssdRunner implements UssdExecutor {

    private static final String ENCODED_HASH = Uri.encode("#");

    // A run that never produces a capturable reply (failed call, cancelled session, a different dialog
    // layout, ...) must not stay pending forever, otherwise the accessibility service would later read and
    // dismiss an unrelated com.android.phone dialog and store its text as the reply to a stale code.
    // USSD replies can take several seconds on slow networks, so 30 s is generous, but the window is
    // kept small so the service only acts on dialogs that belong to a run the user just started.
    private static final long PENDING_TIMEOUT_MS = 30_000;

    private Context context;
    private Ussd pendingUssd;
    private long pendingStartedAt;

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
        pendingStartedAt = SystemClock.elapsedRealtime();
        String ussdCode = ussd.getCode().replaceAll("#", ENCODED_HASH);
        Uri uri = Uri.parse("tel:" + ussdCode);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Device without telephony (the app is installable there): nothing will answer, so no run is pending.
            pendingUssd = null;
        }
    }

    @Override
    public boolean hasPending() {
        if (pendingUssd == null) {
            return false;
        }
        if (SystemClock.elapsedRealtime() - pendingStartedAt > PENDING_TIMEOUT_MS) {
            pendingUssd = null;
            return false;
        }
        return true;
    }

    @Override
    public void setResponse(String result) {
        if (hasPending()) {
            pendingUssd.setResponse(result);
            App.INSTANCE.ussdStorage.updateUssd(pendingUssd);
            pendingUssd = null;
        }
    }
}
