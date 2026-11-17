# 04 — Platform behaviour: targetSdk 27 → 36

Raising `targetSdk` opts the app into every behaviour change between API 28 and API 36. This file lists those
that touch **this** app (manifest, UI, accessibility service, telephony). Items marked **BLOCKER** stop the build or
the install.

## 1. Manifest fixes (blocking)

| # | Change | Reason |
|---|---|---|
| 1 **BLOCKER** | Remove `package="com.tooploox.ussd"`; set `namespace` in Gradle | AGP 8+ |
| 2 **BLOCKER** | `android:exported="true"` on `UssdListActivity` (it has a MAIN/LAUNCHER intent-filter) | Required for any component with an intent filter when targetSdk ≥ 31; otherwise manifest merge fails / install is rejected |
| 3 **BLOCKER** | `android:exported="true"` on `UssdAccessibilityService` (keeps `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`) | Same rule; the system binds from outside the app and is protected by that permission. `SetupActivity` has no filter → `exported="false"` |
| 4 | Move theme to `<application android:theme="@style/Theme.Ussd">`; drop the per-activity `AppCompat.Light.NoActionBar` | Material FAB needs a Material theme (see [02 §4](02-dependencies.md#-theme-requirement-for-the-material-fab)) |
| 5 | Add `android:allowBackup`/`android:dataExtractionRules` + `android:fullBackupContent` decisions | Targeting 31+ lint warns; the app stores user codes in SharedPreferences — decide whether they should be backed up (note serialized-object compatibility) |
| 6 | Add `android:roundIcon` + adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml`, monochrome layer for themed icons on Android 13+) | Current icons are PNG-only; optional but expected |
| 7 | Consider `android:supportsRtl="true"` | Lint/RTL; layouts use start/end after cleanup |

Target manifest skeleton:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <application
        android:name=".App"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Ussd"
        android:allowBackup="false"   <!-- or configure rules explicitly -->
        android:supportsRtl="true">

        <activity android:name=".ui.UssdListActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".ui.SetupActivity" android:exported="false" />

        <service
            android:name=".accessibility.UssdAccessibilityService"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data android:name="android.accessibilityservice" android:resource="@xml/service_config" />
        </service>
    </application>
</manifest>
```

## 2. Edge-to-edge (API 35+ enforced)
With `targetSdk ≥ 35` activities draw behind the status and navigation bars; opting out is not possible from API 36.
Affected screens:
- `activity_ussd_list.xml`: FAB (`layout_gravity="end|bottom"`) and the bottom of the list will sit under the
  gesture/nav bar.
- `activity_setup.xml`: top content will sit under the status bar (no toolbar, fixed top margins).

Fix: call `EdgeToEdge.enable(this)` (`androidx.activity`) in `onCreate` before `setContentView`, and apply insets,
e.g. `ViewCompat.setOnApplyWindowInsetsListener(root, …)` to add system-bar padding to the root/list
(`clipToPadding=false` on the RecyclerView) and margin to the FAB. Dialog (`AlertDialog`) needs nothing.
There is an `edge-to-edge` Claude skill available in this environment for the Compose case; for Views follow the
official guide ("Display content edge-to-edge in views").

## 3. Other behaviour changes relevant to this app

| API | Change | Impact |
|---|---|---|
| 23 | Runtime permissions | Already handled (`SetupHelper`). With minSdk 23 the legacy pre-M path is gone. |
| 26 | Adaptive icons, background limits | Add adaptive icon. No background services in the app. |
| 28 | Network security/https default; `FLAG_ACTIVITY_NEW_TASK` required when starting from non-Activity context | `UssdRunner` already sets `NEW_TASK` (it uses application context). |
| 29/30 | Scoped storage, **package visibility** | Not applicable (no storage; implicit `ACTION_CALL`/`ACTION_ACCESSIBILITY_SETTINGS` launches are not affected by `<queries>`). |
| 31 | `exported` mandatory; PendingIntent mutability | See §1. No PendingIntents used. |
| 33 | `AccessibilityNodeInfo.recycle()` is a no-op/deprecated | We never recycle; fine. |
| 34 | Restrictions on starting activities from the background; implicit intents to non-exported components | The call is launched from a foreground tap — fine. |
| 35 | Edge-to-edge enforced | §2. |
| 36 | Predictive back on by default; large-screen orientation/resizability restrictions ignored | App has no `onBackPressed`/orientation locks — verify nothing breaks on tablets/foldables (list + FAB layout). |
| 16 KB pages (Play requirement for native libs) | n/a | No native code. |
| Dark mode | Material 3 `DayNight` theme will follow system | Check icon assets (`ic_add/ic_enabled/ic_disabled` are PNGs; tint if needed). |

## 4. The accessibility-based USSD capture — risk analysis
`UssdAccessibilityService` relies on: package `com.android.phone`, view ids `android:id/message` /
`android:id/button1`, `TYPE_WINDOW_STATE_CHANGED`. Considerations when targeting current Android:

1. **Robustness fixes (cheap, do them in Phase 3):**
   - null-check `event.getSource()` and `getText()`;
   - only act when a USSD run is pending (`pendingUssd != null`) so unrelated phone-package dialogs aren't dismissed;
   - add `android:accessibilityFlags="flagReportViewIds"` and verify view-id lookup on a real device (the
     current config sets no flags, and view-id lookup is documented to require this one);
   - optionally set `android:notificationTimeout` (e.g. 100 ms) to reduce duplicate events.
2. **OEM variation:** Samsung/Xiaomi/Huawei and dual-SIM flows may use other packages/ids (e.g. `com.android.server.telecom`,
   OEM dialer). Making `packageNames` configurable or removing the filter and matching by dialog structure are
   options; test at least on one Pixel-class device and one OEM device.
3. **Better API where available — `TelephonyManager.sendUssdRequest(String, UssdResponseCallback, Handler)`**
   (API 26): returns the USSD response directly to the app, requires only `CALL_PHONE`, and needs **no
   accessibility service** at all. Limitations: single request/response (no interactive multi-step menus
   in the callback API as far as the public docs describe), uses the default voice SIM unless you obtain a
   `TelephonyManager.createForSubscriptionId(...)` (needs `READ_PHONE_STATE` for subscription lookup), OEM/carrier support
   varies, and failure codes must be handled. Recommended design: implement `UssdExecutor` twice
   (`TelephonyUssdExecutor` first, accessibility fallback second) behind the existing interface.
   This is the single change that would most reduce platform and Play-policy risk.
4. **Android 13+ restricted settings:** sideloaded installs (not Play) see accessibility services greyed out until the
   user allows "restricted settings" for the app. Document this in the setup screen/README if distributing APKs.

## 5. Google Play policy
- Play restricts non-accessibility uses of `AccessibilityService`. An app that uses it to read other apps' UI needs
  the **Accessibility API declaration** in Play Console and must prominently disclose it; reviewers have rejected
  utility-style uses. Keep the current `accessibility_service_description` honest and prominent (it already
  describes what is read), and provide an in-app disclosure/consent screen before sending users to settings.
  Replacing the service with `sendUssdRequest` (§4.3) removes this exposure.
- Target-API deadline: Play requires updates to target a recent API level (API 35 since Aug 2025; the yearly pattern
  makes API 36 the expected requirement from Aug 2026 — **verify the current requirement on Play Console** before
  choosing 36 vs 37).
- `CALL_PHONE` is a "dangerous" permission but not in the SMS/Call-log restricted group; still declare its use in the
  Data safety form.
- `POLICY.md` currently states the app collects IP addresses, page visits, location and shares anonymised data with
  third parties — none of this exists in the code (no `INTERNET`, no location permission, no analytics SDK).
  Rewrite it to say the app stores USSD codes and replies locally only, uses CALL_PHONE to dial, and reads the
  system USSD dialog through the accessibility service. Keep the URL stable if Play already references it.

## 6. Data compatibility on update (from context.md)
Updating over an existing install keeps SharedPreferences. Therefore:
1. Pin `Ussd.serialVersionUID` to the value computed from the **last released** build before touching tooling.
2. Keep class name/package/fields; keep `-keep class …Ussd` rule if obfuscation is enabled.
3. Make `Serializer.deserialize` failures non-fatal (skip `null`s in `getUssdList`) so one corrupt entry can't crash the list.
4. Test: install the old release APK → add codes → install new build over it → verify list and responses.
