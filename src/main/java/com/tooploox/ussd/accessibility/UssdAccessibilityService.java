package com.tooploox.ussd.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tooploox.ussd.App;
import com.tooploox.ussd.data.UssdRepository;
import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.utils.EventBus;

import java.util.List;

public class UssdAccessibilityService extends AccessibilityService {

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = event.getSource();
        List<AccessibilityNodeInfo> ussdResponseTextViewNodesList = rootNode.findAccessibilityNodeInfosByViewId("android:id/message");
        List<AccessibilityNodeInfo> cancelButtonNodesList = rootNode.findAccessibilityNodeInfosByViewId("android:id/button2");

        if (ussdResponseTextViewNodesList.isEmpty() || cancelButtonNodesList.isEmpty()) {
            return;
        }
        CharSequence ussdReponse = ussdResponseTextViewNodesList.get(0).getText();
        App.INSTANCE.ussdExecutor.setResult(ussdReponse.toString());
        App.INSTANCE.eventBus.sendEvent(EventBus.Event.USSD_RESULT_RECEIVED);
        cancelButtonNodesList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void onInterrupt() {

    }
}