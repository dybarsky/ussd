package com.tooploox.ussd.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tooploox.ussd.App;
import com.tooploox.ussd.data.UssdRepository;
import com.tooploox.ussd.domain.Ussd;

import java.util.List;

public class UssdAccessibilityService extends AccessibilityService {

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = event.getSource();
        List<AccessibilityNodeInfo> ussdResponseTextViewNodesList = rootNode.findAccessibilityNodeInfosByViewId("android:id/message");
        if (!ussdResponseTextViewNodesList.isEmpty()) {
            CharSequence ussdReponse = ussdResponseTextViewNodesList.get(0).getText();
            App.INSTANCE.ussdExecutor.setResult(ussdReponse.toString());
        }
    }

    @Override
    public void onInterrupt() {

    }
}