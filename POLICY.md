**Privacy Policy**

This privacy policy applies to the "Accessibility demo" app (hereby referred to as "Application") for Android devices that was created by Maksym Dybarskyi (hereby referred to as "Service Provider") as an Open Source service under the MIT license. This service is intended for use "AS IS".

The Application is a technical demonstration of the Android AccessibilityService API, made to showcase the developer's technical skills. Saving and dialling USSD codes is the example use case it uses for the demonstration.

In short: the Application keeps everything on your device. It has no Internet access, sends no data anywhere, and the Service Provider does not receive or hold any of your data from the Application.

**Information Collection and Use**

The Service Provider does not collect any information from you. The Application does not transmit any information off your device. The only information the Service Provider can see about the Application is what Google Play itself makes available to developers, for example aggregated install statistics and crash reports from users who chose to share diagnostics with Google. That information comes from Google Play, not from the Application.

The Application stores the following information locally on your device only, in the Application's private storage:

*   The USSD codes (quick codes) that you add to the Application
*   The last response text received for each of those codes

This information is used only to display your saved codes and their last responses inside the Application. The Application does not request an account, and does not ask for your name, contacts, or any other personal information.

The Application contains no advertising, no usage tracking, no crash reporting, and no tracking of where you are, and it does not use any third-party service. The only third-party code it includes is the open-source AndroidX and Material Components user interface libraries, and because the Application has no Internet permission, no code in it can send data off your device.

**Permissions and Accessibility Service**

*   **Phone (CALL_PHONE):** This is the only permission you are asked to grant. It is used to dial the USSD codes that you saved, when you tap one. The Application does not read your call history, contacts, or phone state. Depending on your device, the system phone app may itself record dialled codes in its call history; that is done by the system, not by the Application.
*   **Accessibility service:** When you run a saved USSD code, the Android system shows the operator's reply in a system dialog. The Application uses an Android AccessibilityService, restricted to the system phone package (com.android.phone), to read the text of that USSD response dialog and to click its dismiss (OK) button. This happens only while a run started from the Application is waiting for its reply, which is at most 30 seconds after you tap a code. The text read is stored locally on your device as the response for that code and is shown to you in the Application.

Before the Application sends you to your device's accessibility settings, it shows a separate screen that explains this use of the AccessibilityService, and the service only reads a USSD reply after you tap "Agree" on that screen. You can tap "No thanks" instead. The Application then still dials your saved codes, but it does not capture or show the replies, and you can reopen the screen later from the banner in the list. The Application uses the AccessibilityService API only for the purpose described here.

The accessibility service is limited to the system phone package. Android delivers window-change events from that package to the service, and the service is technically permitted to read the content of those windows. The Application ignores these events unless a run it started is waiting for a reply, and even then it reads only the message text and the OK button of the reply dialog. It does not read the content of any other app or screen, does not record keystrokes, and does not transmit anything. The text it reads is not shared with anyone. You must enable the service manually in your device's system accessibility settings, and you can disable it there at any time. Without it, the Application cannot capture USSD responses.

When a USSD code is dialled, it is sent to your mobile network operator by your phone's normal telephony system, in the same way as if you had dialled it yourself. The Application itself does not send it anywhere. The operator's own privacy practices apply to that traffic.

**Third Party Access**

The Service Provider does not share your information with anyone, because the Service Provider does not receive it. The Application has no Internet permission and does not send data to any third party.

**Opt-Out Rights**

You can stop the Application from reading USSD response dialogs at any time by disabling its accessibility service in your device's system accessibility settings. You can revoke the phone permission in your device's app settings. You can also stop all use of the Application by uninstalling it, using the standard uninstall process of your device or app store.

**Data Retention Policy**

The data described above stays on your device until you remove it. Uninstalling the Application or clearing its storage in your device's settings deletes all of its data. Automatic backup of the Application's data is disabled, so the Application does not back it up to the cloud. Because the Service Provider holds none of your data, there is nothing for the Service Provider to delete on your behalf.

**Children**

The Application does not address anyone under the age of 13, and the Service Provider does not knowingly collect any information from children or from anyone else.

**Security**

The Application's data is stored in app-private storage that is protected by the Android application sandbox, and it is never transmitted from your device. The stored data is not additionally encrypted by the Application. USSD replies can contain sensitive information such as account balances, and they are shown on screen in the Application, so they may also appear in your device's recent-apps preview. The Application has no function to delete a single stored reply; uninstalling the Application or clearing its storage deletes everything. The protection of your device itself, such as a screen lock, is up to you.

**Changes**

This Privacy Policy may be updated from time to time. Any changes will be published by updating this page with the new Privacy Policy. You are advised to consult this Privacy Policy regularly for any changes.

This privacy policy is effective as of 2026-11-18

**Your Consent**

By using the Application, you are consenting to the handling of information as set forth in this Privacy Policy.

**Contact Us**

If you have any questions regarding privacy while using the Application, please contact the Service Provider via email at maxim.dybarskyy@gmail.com.
