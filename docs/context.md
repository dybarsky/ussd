# Context, domain knowledge and gotchas

## Domain
USSD codes ("quick codes", `*123#`) are short GSM session commands to the mobile operator (balance, tariff,
top-up). The OS shows the reply in a system dialog. Android has historically offered no easy API to read that
text, hence this app's accessibility-service trick. The app is a personal utility/demo, not a commercial product.

## Permissions and special access

| Item | Declared where | Notes |
|---|---|---|
| `android.permission.CALL_PHONE` | manifest | Runtime permission (API 23+) requested from `SetupActivity` via `SetupHelper.requestPermissions`. Needed for `Intent.ACTION_CALL`. |
| Accessibility service | manifest `<service>` + `res/xml/service_config.xml` | User must enable it manually in system settings (`Settings.ACTION_ACCESSIBILITY_SETTINGS`). Cannot be granted programmatically. Enabled-state is detected in `SetupHelper.isAccessibilityServiceEnabled`. |
| Network | — | None. The app has no `INTERNET` permission. |

## Android-specific behaviour the code relies on
- The USSD reply dialog belongs to package `com.android.phone` and exposes `android:id/message` and
  `android:id/button1`. This is AOSP behaviour; **OEM dialers/telephony stacks differ** (e.g. Samsung, Xiaomi,
  dual-SIM devices, newer Android versions). Expect "no response captured" bug reports on some devices.
- `service_config.xml` does not set `android:accessibilityFlags="flagReportViewIds"`; lookups by view id are
  documented to need it. Verify on a device when touching this code.
- The dialog is auto-dismissed by clicking `button1` for **any** window in `com.android.phone` that has both ids
  — even if no USSD run is pending.
- Minimum API 16 in the current build, but `findAccessibilityNodeInfosByViewId` is API 18+ (hence
  `@SuppressLint("NewApi")`). Raising minSdk to ≥ 23 (required by modern AndroidX) removes that smell.

## Persistence gotcha
User data = `Base64(ObjectOutputStream(Ussd))` in SharedPreferences `SimpleStorage`.

- `Ussd implements Serializable` with **no explicit `serialVersionUID`**. Java then derives one from the class
  shape. Renaming/moving the class, changing its fields, or (rarely) a compiler change can make old data
  unreadable.
- `Serializer.deserialize` swallows `IOException`/`ClassNotFoundException` and returns `null`;
  `UssdSimpleStorage.getUssdList` adds that `null` to the list → later NPE in the adapter/data binding.
- **Before any release that changes tooling**, compute the current class's UID from the last released build
  (`serialver -classpath <classes> com.tooploox.ussd.domain.Ussd`) and pin it with
  `private static final long serialVersionUID = <value>L;`, or write an explicit migration to a new storage.
- Do not move `Ussd` out of `com.tooploox.ussd.domain` unless you add a migration.
- Order of entries is unstable (`SharedPreferences.getAll()` is a map).

## Known quirks / bugs (found during analysis; none fixed yet)

| # | Where | Issue | Severity |
|---|---|---|---|
| 1 | `UssdAccessibilityService.onAccessibilityEvent` | `event.getSource()` can be `null` → NPE; `getText()` can be `null` → NPE on `toString()`; source nodes are never `recycle()`d (deprecated no-op on API 33+) | Medium |
| 2 | `UssdRunner` | `pendingUssd` is in-memory only; replies after process death are dropped; a second tap overwrites the first | Low |
| 3 | `UssdRunner.run` | Silently returns when permission is missing (no user feedback) | Low |
| 4 | `EventBus.unsubscribe` | `unregisterReceiver(null)` if never subscribed; map holds one receiver per event | Low |
| 5 | `Serializer` / `UssdSimpleStorage` | Null entries on deserialization failure, see above | Medium |
| 6 | `UssdListActivity` | `presenter` field initialised from `App.INSTANCE` at construction; `ButterKnife.bind(dialogViews, dialog)` re-binds on each dialog show | Low |
| 7 | `AndroidManifest.xml` | No `android:exported` on launcher activity/service → hard error at targetSdk ≥ 31; also uses `package=` instead of Gradle `namespace` | **Blocker for upgrade** |
| 8 | `proguard-android.txt` | Hand-copied legacy default rules containing `-dontoptimize`/`-dontobfuscate`; name shadows the SDK file AGP 9 refuses to use as a default | Medium for upgrade |
| 9 | `POLICY.md` | Privacy policy claims IP address/location/analytics collection and third-party sharing; app does none of that (no network, no location permission). Either fix it or Play review/data-safety form will be inconsistent | Medium (publishing) |
| 10 | `src/main/res/res/` | Accidental nested directory with unreferenced alternate (less-padded) renderings of `ic_enabled`/`ic_disabled` — not byte-identical duplicates. **Removed** in the upgrade; recoverable from git history | Trivial |
| 11 | `build.gradle` | `storePassword keystorePassword` references a project property that exists only as an empty placeholder in committed `gradle.properties` | Low |
| 12 | Resources | Legacy `layout_marginLeft/Right` and `left/right` constraints (RTL lint), pre-adaptive launcher icons (PNG mipmaps only), no dark theme, English-only strings with typos ("quite codes" in `accessibility_service_summary`) | Low |
| 13 | Layout | `ussd_list_item.xml` uses `0dp` widths + `layout_constraintHorizontal_weight` but the two TextViews are only constrained one-way (no right-side constraints), so they do not form a real chain; rendering may differ between ConstraintLayout 1.0.2 and 2.x — check visually after the upgrade (not verified) | Low |

## Distribution context
- Open source repo, MIT license (`LICENSE`), privacy policy in `POLICY.md` (used as the Play listing policy URL).
- Google Play restricts apps that use the AccessibilityService API for non-accessibility purposes and requires
  a declaration/justification; this is the biggest non-technical risk for keeping the app on Play. See
  [upgrade/04-platform-behavior.md](upgrade/04-platform-behavior.md#google-play-policy).
- Release signing: keystore expected at `~/.android/dmax.keystore`, alias `dmax`; password via Gradle property
  `keystorePassword`. The keystore is not in the repo. Losing it matters only if Play App Signing is not used.
- History: authored in 2016–2017 (comment headers); last real code change era = Support Library 27.

## Glossary
- **USSD** — Unstructured Supplementary Service Data.
- **Accessibility service** — privileged service that receives UI events from other apps and can click on their UI.
- **App.INSTANCE** — static application singleton used as service locator.
