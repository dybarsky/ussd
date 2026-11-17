**Privacy Policy**

This privacy policy applies to the Accessibility service demo app (hereby referred to as "Application") for Android devices that was created by Maksym Dybarskyi (hereby referred to as "Service Provider") as an Open Source service under the MIT license. This service is intended for use "AS IS".

In short: the Application keeps everything on your device. It has no Internet access, sends no data anywhere, and the Service Provider does not receive or hold any of your data.

**Information Collection and Use**

The Service Provider does not collect any information from you. The Application does not transmit any information off your device.

The Application stores the following information locally on your device only, in the Application's private storage:

*   The USSD codes (quick codes) that you add to the Application
*   The last response text received for each of those codes

This information is used only to display your saved codes and their last responses inside the Application. The Application does not request an account, and does not ask for your name, contacts, or any other personal information.

The Application contains no advertising, no usage tracking, no crash reporting, and no tracking of where you are, and it does not use any third-party service. The only third-party code it includes is the open-source AndroidX and Material Components user interface libraries, and because the Application has no Internet permission, no code in it can send data off your device.

**Permissions and Accessibility Service**

*   **Phone (CALL_PHONE):** This is the only permission the Application declares. It is used to dial the USSD codes that you saved, when you tap one. The Application does not read your call history, contacts, or phone state.
*   **Accessibility service:** When you run a saved USSD code, the Android system shows the operator's reply in a system dialog. The Application uses an Android AccessibilityService, restricted to the system phone package (com.android.phone), to read the text of that USSD response dialog and to click its dismiss (OK) button. This happens only while a run started from the Application is pending. The text read is stored locally on your device as the response for that code and is shown to you in the Application.

The accessibility service does not read the content of any other app or screen, does not record keystrokes, and does not transmit anything. The text it reads is not shared with anyone. You must enable the service manually in your device's system accessibility settings, and you can disable it there at any time. Without it, the Application cannot capture USSD responses.

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

The Application's data is stored in app-private storage that is protected by the Android application sandbox, and it is never transmitted from your device. The protection of your device itself, such as a screen lock, is up to you.

**Changes**

This Privacy Policy may be updated from time to time. Any changes will be published by updating this page with the new Privacy Policy. You are advised to consult this Privacy Policy regularly for any changes.

This privacy policy is effective as of 2026-11-17

**Your Consent**

By using the Application, you are consenting to the handling of information as set forth in this Privacy Policy.

**Contact Us**

If you have any questions regarding privacy while using the Application, please contact the Service Provider via email at maxim.dybarskyy@gmail.com.
