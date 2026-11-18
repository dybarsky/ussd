# Google Play listing and Play Console checklist

Text for the store listing and the steps in Play Console that go with the version that adds the in-app
accessibility disclosure (`versionCode 4`, `0.4`). Character limits are Google Play's.

## Listing text

**Title** (≤ 30): `Accessibility demo: USSD`

**Short description** (≤ 80): `Demo of the Android Accessibility Service, using USSD codes as an example.`

**Full description** (≤ 4000):

```
Accessibility demo is a technical demonstration of the Android Accessibility Service API, made to showcase the developer's technical skills. Saving and dialling USSD codes (quick codes such as *100#) is used as an example use case.

HOW IT WORKS
1. Add a USSD code to the list.
2. Tap it. Your phone dials it as if you had typed it.
3. When the operator answers, the accessibility service reads the reply text from the system dialog, shows it next to the code in the app and closes the dialog.

WHY THE APP USES THE ACCESSIBILITY SERVICE
Showing how the AccessibilityService API works is the purpose of this app. It is used for one thing only: reading the text of the USSD reply dialog from the system phone app and pressing its OK button so the dialog closes.
• It only runs for up to 30 seconds after you tap a saved code in this app.
• It only looks at dialogs from the system phone app. It does not read any other app or screen, does not record what you type, and does not collect passwords or personal data.
• The reply text is stored on your device only and is never sent anywhere.
• The app asks for your consent inside the app before it takes you to Android's Accessibility settings. You can say no and still dial your codes, but replies will not be captured. You can turn the service off at any time in Settings > Accessibility.
This app is not an assistive tool for people with disabilities. It uses the Accessibility API only for the purpose described above.

PERMISSIONS
• Phone: used only to dial the USSD codes you saved, when you tap them. The app does not read your contacts, call history or phone state.
• Accessibility service: described above, only after you agree.

PRIVACY
No internet access, no ads, no analytics, no account. Everything stays on your device. Privacy policy: <PRIVACY POLICY URL>

Your mobile operator's own terms and charges apply to the USSD codes you run. The app is not affiliated with any mobile operator.
```

Replace `<PRIVACY POLICY URL>` with the public URL of `POLICY.md` (the same URL as in the Play Console privacy
policy field). The full description above is about 1,900 characters.

## Graphic assets

| Asset | File | Notes |
|---|---|---|
| App icon 512×512 | `docs/store/ic_play_store_512.png` | Full-bleed square, Google applies the mask. Source: `docs/store/icon.svg` |
| Feature graphic 1024×500 | not produced yet | Required by Play. Needs to be made before submitting a new listing |
| Screenshots | not produced yet | Take them from a phone: wizard steps 1–3, the list with saved codes and replies |

The launcher icon in the app (`res/mipmap-anydpi-v26/ic_launcher.xml`, `res/drawable/ic_launcher_foreground.xml`,
`res/mipmap-*/ic_launcher.png`) uses the same artwork.

## Play Console steps

1. **App content → Accessibility services declaration**
   - Why does your app need the Accessibility Services API: **App functionality**.
   - Do you collect and/or share personal or sensitive data using the accessibility capabilities: **No**. The reply text
     is stored on the device only and never leaves it.
   - Demo video: link to an unlisted YouTube video (see below).
   - Do **not** mark the app as an accessibility tool (`isAccessibilityTool` is not set).
2. **Demo video**, recorded on a physical phone, showing in one take:
   1. Opening the app on a fresh install (clear data first).
   2. Wizard step 1, step 2, then step 3, the full disclosure. If it needs scrolling, scroll slowly so all text is readable.
   3. Tapping **Agree and open settings**, turning the service on in Android's Accessibility settings.
   4. Saving a code, tapping it, and the reply appearing next to it (the core feature).
   5. The decline path: clear data, go through the wizard again, tap **No thanks**, then use the banner on the list screen
      to open the disclosure again.
3. **Store listing**: new description, icon, feature graphic and screenshots.
4. **Data safety**: no data collected, no data shared. **Privacy policy URL**: public URL of `POLICY.md`.
5. Upload the signed release build (`./gradlew assembleRelease`; see AGENTS.md for the signing setup), then reply to the
   policy email: a prominent in-app disclosure with an explicit Agree / No thanks choice was added in version 0.4,
   shown before the user is sent to the Accessibility settings.
