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
    public static final String BUTTON_VIEW_ID = "android:id/button1";

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = event.getSource();
        // find textview with message && button
        List<AccessibilityNodeInfo> ussdResponseTextViewNodesList = rootNode.findAccessibilityNodeInfosByViewId(MESSAGE_VIEW_ID);
        List<AccessibilityNodeInfo> cancelButtonNodesList = rootNode.findAccessibilityNodeInfosByViewId(BUTTON_VIEW_ID);

        // in dialog doesn't contain views - skip accessibility dialog
        if (ussdResponseTextViewNodesList.isEmpty() || cancelButtonNodesList.isEmpty()) {
            return;
        }
        // get text from textview
        CharSequence ussdReponse = ussdResponseTextViewNodesList.get(0).getText();
        App.INSTANCE.ussdExecutor.setResponse(ussdReponse.toString());
        App.INSTANCE.eventBus.sendEvent(EventBus.Event.USSD_RESULT_RECEIVED);
        // dismiss dialog
        cancelButtonNodesList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void onInterrupt() {

    }
}