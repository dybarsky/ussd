# Context, domain knowledge and gotchas

Status as of 2026-11-17 on branch `modernize/agp-9` (commits referenced by hash are on that branch).

## Domain
USSD codes ("quick codes", `*123#`) are short GSM session commands to the mobile operator (balance, tariff,
top-up). The OS shows the reply in a system dialog. Android has historically offered no easy API to read that
text, hence this app's accessibility-service trick. The app is a personal utility/demo, not a commercial product.

## Permissions and special access

| Item | Declared where | Notes |
|---|---|---|
| `android.permission.CALL_PHONE` | manifest | Runtime permission requested from `SetupActivity` via `SetupHelper.requestPermissions`. Needed for `Intent.ACTION_CALL`. |
| Accessibility service | manifest `<service>` (`exported=true`, protected by `BIND_ACCESSIBILITY_SERVICE`) + `res/xml/service_config.xml` | User must enable it manually in system settings (`Settings.ACTION_ACCESSIBILITY_SETTINGS`). Cannot be granted programmatically. Enabled-state is detected in `SetupHelper.isAccessibilityServiceEnabled`. |
| Network | — | None. The app has no `INTERNET` permission. |

## Android-specific behaviour the code relies on
- The USSD reply dialog belongs to package `com.android.phone` and exposes `android:id/message` and
  `android:id/button1`. This is AOSP behaviour; **OEM dialers/telephony stacks differ** (e.g. Samsung, Xiaomi,
  dual-SIM devices, newer Android versions). Expect "no response captured" bug reports on some devices.
  Not verified on any real SIM device or OEM build.
- `service_config.xml` now sets `android:accessibilityFlags="flagReportViewIds"` (needed for lookups by view id)
  and `android:notificationTimeout="100"` (commit 91de773). Effect on real devices is not verified.
- The service only reads/dismisses a dialog while `UssdExecutor.hasPending()` is true, i.e. within 30 s after the
  app itself started a run in this process (`UssdRunner.PENDING_TIMEOUT_MS`; a run that never gets a reply expires
  instead of staying pending forever, so a stale run can no longer swallow an unrelated dialog). Unrelated
  `com.android.phone` dialogs are ignored. Consequences: if the process was killed between run and reply, or the
  reply arrives later than 30 s, the reply is not captured and the dialog is not auto-dismissed.
- minSdk is 23, so `findAccessibilityNodeInfosByViewId` (API 18+) needs no `@SuppressLint("NewApi")` any more
  (removed).
- Android 13+ restricted settings can grey out the accessibility service for sideloaded APKs (see
  [upgrade/04](upgrade/04-platform-behavior.md#4-the-accessibility-based-ussd-capture--risk-analysis)).

## Persistence gotcha
User data = `Base64(ObjectOutputStream(Ussd))` in SharedPreferences `SimpleStorage`.

- `Ussd` now has a **pinned** `private static final long serialVersionUID = -2632603246578927450L;` (commit
  574984d). The value was computed from the original (tag `0.2`) class with `javac --release 8` and with JDK 25's
  `javac`; both gave the same value. It is **not** derived from the class shape any more, but renaming/moving
  the class or changing/removing existing fields can still break old data. Adding fields is compatible.
- `proguard-rules.pro` keeps `com.tooploox.ussd.domain.Ussd` (members and name) because R8 now obfuscates.
- `Serializer.deserialize` still swallows `IOException`/`ClassNotFoundException` and returns `null`, but
  `UssdSimpleStorage.getUssdList` now skips `null` results, non-String values and entries that throw a
  `RuntimeException` (e.g. bad Base64), so one bad entry no longer breaks the list (commit 062dcbf).
- The list is now sorted: code (case-insensitive, nulls last), then id. There is no creation timestamp in the
  model, so this is alphabetical, not insertion order.
- Do not move `Ussd` out of `com.tooploox.ussd.domain` unless you add a migration.
- **Not verified:** reading data written by the real old release APK with the new (R8-obfuscated) release build
  (upgrade-over-old-APK test, see [upgrade/05](upgrade/05-plan-and-verification.md)). `allowBackup` is `false`
  on purpose, so stored data is not restored from backups.

## Known quirks / bugs

Status column: **Fixed** = fixed on this branch (commit subject given), **Open** = still true, **Partial**.
Original descriptions are kept for history.

| # | Where | Issue | Severity | Status |
|---|---|---|---|---|
| 1 | `UssdAccessibilityService.onAccessibilityEvent` | `event.getSource()` can be `null` → NPE; `getText()` can be `null` → NPE on `toString()`; source nodes are never `recycle()`d (deprecated no-op on API 33+) | Medium | **Fixed** — "Harden accessibility service against null nodes and stray dialogs" (91de773); null checks added; nodes still not recycled (no-op, deliberate) |
| 2 | `UssdRunner` | `pendingUssd` is in-memory only; replies after process death are dropped; a second tap overwrites the first. A run that got no reply used to stay pending forever (the service would then read and dismiss any later phone dialog with `message`+`button1`); the pending window is now bounded to 30 s | Low | Partial: stale-pending fixed (30 s expiry, JVM-harness verified); in-memory loss and 30 s limit remain (reply after 30 s or process death is not captured and the dialog is not auto-dismissed) |
| 3 | `UssdRunner.run` | Silently returns when permission is missing (no user feedback) | Low | Open |
| 4 | `EventBus.unsubscribe` | `unregisterReceiver(null)` if never subscribed; map holds one receiver per event | Low | Open |
| 5 | `Serializer` / `UssdSimpleStorage` | Null entries on deserialization failure; unstable order | Medium | **Fixed** — "Skip unreadable entries when loading stored USSD codes" (062dcbf); `Serializer` itself unchanged |
| 6 | `UssdListActivity` | `presenter` field initialised from `App.INSTANCE` at construction; `ButterKnife.bind(dialogViews, dialog)` re-binds on each dialog show | Low | **Partial** — ButterKnife gone ("Replace ButterKnife with ViewBinding", 16e6304); field initialisation from `App.INSTANCE` unchanged |
| 7 | `AndroidManifest.xml` | No `android:exported` on launcher activity/service → hard error at targetSdk ≥ 31; uses `package=` instead of Gradle `namespace` | **Blocker for upgrade** | **Fixed** — "Migrate build to Gradle 9.6.1 and AGP 9.5.1" (d0bdd1c, `namespace`, `package` removed) and "Declare android:exported and backup policy in manifest" (c276ecd) |
| 8 | `proguard-android.txt` | Hand-copied legacy default rules with `-dontoptimize`/`-dontobfuscate`; name shadows the SDK file AGP 9 refuses to use as a default | Medium for upgrade | **Fixed** — "Switch to R8 optimize defaults with keep rule for Ussd" (4513802); file deleted, `proguard-rules.pro` added. R8 now optimizes/obfuscates and inlines `UssdRunner` into its callers (normal) |
| 9 | `POLICY.md` | Privacy policy claimed IP address/location/analytics collection and third-party sharing the app does not do | Medium (publishing) | **Fixed** — "Align privacy policy with actual app behavior" (faf7ab9); also fixed the "quite codes" typo in `accessibility_service_summary` |
| 10 | `src/main/res/res/` | Accidental nested directory with unreferenced alternate (less-padded) renderings of `ic_enabled`/`ic_disabled` — not byte-identical duplicates | Trivial | **Fixed** — "Remove accidental nested res/res directory" (ac267c5); recoverable from git history |
| 11 | `build.gradle` | `storePassword keystorePassword` referenced a project property that existed only as an empty placeholder in committed `gradle.properties` | Low | **Fixed** — d0bdd1c: placeholder removed; signing config reads `keystorePassword` property or `KEYSTORE_PASSWORD`, and `KEYSTORE_FILE` (default `~/.android/dmax.keystore`), and only signs if both are present |
| 12 | Resources | Legacy `layout_marginLeft/Right` and `left/right` constraints (RTL lint), pre-adaptive launcher icons (PNG mipmaps only), no dark theme, English-only strings with typos | Low | **Partial** — start/end attributes and `supportsRtl="true"` (6ec2a46, c276ecd); `Theme.Material3.DayNight` (visuals in dark mode not checked); typo fixed (faf7ab9). Still open: PNG-only launcher icon (no adaptive/monochrome), English only, RTL/large-font/tablet not visually verified |
| 13 | Layout | `ussd_list_item.xml` uses `0dp` widths + `layout_constraintHorizontal_weight` but the two TextViews are only constrained one-way, so they do not form a real chain; rendering may differ between ConstraintLayout 1.0.2 and 2.x | Low | Open (layout structure unchanged; not checked visually on 2.2.2) |
| 14 | UI | **New.** The UI has no way to delete a saved code: `UssdRepository.removeUssd` is implemented but never called from production code | Low | Open |
| 15 | `dialog_add_ussd.xml` / `UssdListActivity` | **New.** The add dialog does not auto-focus the input or show the keyboard (no `requestFocus`/soft-input handling in code). On an API 37 emulator Gboard's `inputType=phone` dialpad did not type `*` or `#`, and `*`/`#` insert buttons were added (0488f7a); on the maintainer's physical phone Gboard types them fine, so the buttons were removed again ("Remove * and # buttons from the add-USSD dialog") | Low | Open (auto-focus only; `*`/`#` entry is keyboard-dependent and was not a problem on the physical device) |
| 16 | Theme | **New.** Material 3 default accent colours replaced the old AppCompat accent; the app never defined brand colours (`colors.xml` was removed as unused). The list code text uses `?attr/colorSecondary`; look changed from before | Low | Open (cosmetic, by design for now) |
| 17 | `lint-baseline.xml` | **New.** 10 deferred findings, see below | Low | Open |

### Deferred lint findings (`lint-baseline.xml`, generated by lint 9.5.1)
1. `PermissionImpliesUnsupportedChromeOsHardware` (manifest) — `CALL_PHONE` implies telephony hardware; would need `<uses-feature android.hardware.telephony required="false">`.
2. `ObsoleteLintCustomCheck` — a `lifecycle-runtime-ktx-2.6.2` lint jar built for an older lint API (third-party, skipped checks).
3. `OldTargetApi` (`build.gradle`) — targetSdk 36 while 37 exists.
4. `RedundantLabel` (manifest) — the `UssdListActivity` label duplicates the application label.
5. `AndroidGradlePluginVersion` (`gradle-wrapper.properties`) — newer Gradle available (9.9.1; message says "newer Gradle than 9.6.1").
6. `GradleDependency` (`build.gradle`) — newer `compileSdk` (37) available.
7. `DataExtractionRules` (manifest) — `allowBackup` deprecated on Android 12+, suggests `dataExtractionRules`.
8. `NotifyDataSetChanged` (`UssdListAdapter`) — `notifyDataSetChanged()` used on every update.
9. `NotShrinkingResources` (`build.gradle`) — `shrinkResources = false` (reported twice, two entries).

That is 10 entries (items 1–8 plus the two `NotShrinkingResources` entries). New lint issues fail the build
(`abortOnError = true`).

## Distribution context
- Open source repo, MIT license (`LICENSE`), privacy policy in `POLICY.md` (used as the Play listing policy URL;
  rewritten in faf7ab9 to match actual behaviour).
- Google Play restricts apps that use the AccessibilityService API for non-accessibility purposes and requires
  a declaration/justification; this is the biggest non-technical risk for keeping the app on Play. See
  [upgrade/04-platform-behavior.md](upgrade/04-platform-behavior.md#5-google-play-policy). Play Console steps
  (Accessibility API declaration, Data safety form, target-API requirement) are **not done**; `versionCode` was
  already bumped to 3 / `versionName` 0.3.
- Release signing: keystore expected at `~/.android/dmax.keystore` (override with `KEYSTORE_FILE`), alias `dmax`;
  password via Gradle property `keystorePassword` (e.g. in `~/.gradle/gradle.properties`) or `KEYSTORE_PASSWORD`.
  The keystore is not in the repo. Signing was exercised only locally (a local release build was signed with the
  real key); CI signing is not set up. Losing the keystore matters only if Play App Signing is not used.
- History: authored in 2016–2017 (comment headers); modernized to AGP 9 / AndroidX / API 36 in 2026-11 on branch
  `modernize/agp-9`.

## Glossary
- **USSD** — Unstructured Supplementary Service Data.
- **Accessibility service** — privileged service that receives UI events from other apps and can click on their UI.
- **App.INSTANCE** — static application singleton used as service locator.
