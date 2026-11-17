package com.tooploox.ussd.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tooploox.ussd.App;
import com.tooploox.ussd.utils.EventBus;

import java.util.List;

public class UssdAccessibilityService extends AccessibilityService {

    private static final String MESSAGE_VIEW_ID = "android:id/message";
    public static final String BUTTON_VIEW_ID = "android:id/button1";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Only act on dialogs that belong to a USSD run started by this app, so unrelated
        // com.android.phone dialogs are never read or dismissed. Known consequence: the pending
        // state is in memory only, so if the process was killed between run and reply, the
        // dialog is not auto-dismissed.
        if (!App.INSTANCE.ussdExecutor.hasPending()) {
            return;
        }
        if (event == null) {
            return;
        }
        AccessibilityNodeInfo rootNode = event.getSource();
        if (rootNode == null) {
            return;
        }
        // find textview with message && button
        List<AccessibilityNodeInfo> ussdResponseTextViewNodesList = rootNode.findAccessibilityNodeInfosByViewId(MESSAGE_VIEW_ID);
        List<AccessibilityNodeInfo> cancelButtonNodesList = rootNode.findAccessibilityNodeInfosByViewId(BUTTON_VIEW_ID);

        // in dialog doesn't contain views - skip accessibility dialog
        if (ussdResponseTextViewNodesList == null || ussdResponseTextViewNodesList.isEmpty()
                || cancelButtonNodesList == null || cancelButtonNodesList.isEmpty()) {
            return;
        }
        AccessibilityNodeInfo messageNode = ussdResponseTextViewNodesList.get(0);
        AccessibilityNodeInfo buttonNode = cancelButtonNodesList.get(0);
        if (messageNode == null || buttonNode == null) {
            return;
        }
        // get text from textview
        CharSequence ussdReponse = messageNode.getText();
        if (ussdReponse == null) {
            return;
        }
        App.INSTANCE.ussdExecutor.setResponse(ussdReponse.toString());
        App.INSTANCE.eventBus.sendEvent(EventBus.Event.USSD_RESULT_RECEIVED);
        // dismiss dialog
        buttonNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void onInterrupt() {

    }
}
