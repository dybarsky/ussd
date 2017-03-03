package com.tooploox.ussd.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tooploox.ussd.App;
import com.tooploox.ussd.utils.EventBus;

import java.util.List;

public class UssdAccessibilityService extends AccessibilityService {

    private static final String MESSAGE_VIEW_ID = "android:id/message";
    public static final String CANCEL_BUTTON_VIEW_ID = "android:id/button2";

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = event.getSource();
        List<AccessibilityNodeInfo> ussdResponseTextViewNodesList = rootNode.findAccessibilityNodeInfosByViewId(MESSAGE_VIEW_ID);
        List<AccessibilityNodeInfo> cancelButtonNodesList = rootNode.findAccessibilityNodeInfosByViewId(CANCEL_BUTTON_VIEW_ID);

        if (ussdResponseTextViewNodesList.isEmpty() || cancelButtonNodesList.isEmpty()) {
            return;
        }
        CharSequence ussdReponse = ussdResponseTextViewNodesList.get(0).getText();
        App.INSTANCE.ussdExecutor.setResponse(ussdReponse.toString());
        App.INSTANCE.eventBus.sendEvent(EventBus.Event.USSD_RESULT_RECEIVED);
        cancelButtonNodesList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void onInterrupt() {

    }
}