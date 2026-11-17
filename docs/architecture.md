# Architecture

A small, hand-wired **MVP-ish** app. There is no DI framework, no Jetpack ViewModel/Room/Navigation, no
coroutines/RxJava. Everything runs on the main thread (the accessibility callback is also delivered on it).
Views are bound with **ViewBinding** (activities, dialog) and **DataBinding** (list item); no ButterKnife.

Status: this file describes the code after the AGP 9 / AndroidX upgrade (branch `modernize/agp-9`).

## Layers and packages (`com.tooploox.ussd`)

```
┌──────────────────────────── ui ─────────────────────────────┐
│ UssdListActivity (launcher)  SetupActivity (not exported)   │
│ UssdListAdapter ──implements──► UssdViewModel (interface)   │
│ UssdListPresenter                                           │
└───────────────┬──────────────────────────────┬──────────────┘
                │ uses                         │ uses
┌───────────────▼─────── data ────────┐  ┌─────▼───────── utils ─────────┐
│ UssdRepository  (interface)         │  │ SetupHelper (permission +     │
│   UssdSimpleStorage (SharedPrefs)   │  │   accessibility checks)       │
│   UssdRuntimeStorage (in-memory,    │  │ EventBus (LocalBroadcast)     │
│     unused)                         │  │ Strings, Predicate,           │
│ UssdExecutor    (interface)         │  │ ItemViewPositionProvider      │
│   UssdRunner (ACTION_CALL intent)   │  └───────────────────────────────┘
└───────────────▲─────────────────────┘
                │ setResponse() + EventBus
┌───────────────┴──── accessibility ──┐        ┌──── domain ────┐
│ UssdAccessibilityService            │        │ Ussd (POJO,    │
│  (reads system USSD dialog)         │        │ Serializable)  │
└─────────────────────────────────────┘        └────────────────┘
```

### Composition root — `App.java`
`App extends Application` creates three singletons and exposes them through a **static `App.INSTANCE`**:

| Field | Type | Implementation |
|---|---|---|
| `ussdStorage` | `UssdRepository` | `UssdSimpleStorage` |
| `ussdExecutor` | `UssdExecutor` | `UssdRunner` |
| `eventBus` | `EventBus` | `LocalBroadcastManager` wrapper |

Everything that needs them (activities, the accessibility service) reaches for `App.INSTANCE.*`. This is a
service locator, not DI; it is the single seam to replace if you introduce Hilt/Koin or constructor injection.
Note `INSTANCE` is assigned **last** in `onCreate`.

### Domain — `domain/Ussd`
Mutable POJO: `id` (random UUID, identity for `equals/hashCode`), `code` (e.g. `*100#`), `response`
(nullable String, last reply). `Serializable` + hand-written `clone()`. This exact class is what gets
persisted — see persistence below.

### Data
- `UssdRepository`: `addUssd / removeUssd / updateUssd / getUssdList`. (`removeUssd` is implemented but no UI calls it.)
  - `UssdSimpleStorage` — one SharedPreferences file `SimpleStorage`; **key = `Ussd.id`, value = Base64 of
    Java-serialized `Ussd`** (via `ui/Serializer`, which oddly lives in the `ui` package). `updateUssd` = `addUssd`.
    `getUssdList` iterates `prefs.getAll()`, **skips** non-String values, entries that deserialize to `null`
    and entries that throw a `RuntimeException`, then sorts with `ORDER`: code (case-insensitive, nulls last),
    then id. The order is alphabetical because the model has no creation time.
  - `UssdRuntimeStorage` — in-memory list; not referenced by production code (left over from early dev/tests).
- `UssdExecutor`: `run(Ussd)`, `setResponse(String)` and `hasPending()`.
  - `UssdRunner.run`: bails out if `CALL_PHONE` is not granted; stores `pendingUssd = ussd.clone()`; builds
    `tel:` URI with `#` percent-encoded; fires `Intent.ACTION_CALL` with `FLAG_ACTIVITY_NEW_TASK`.
  - `UssdRunner.hasPending`: `pendingUssd != null`; the accessibility service uses it as a gate.
  - `UssdRunner.setResponse`: if a `pendingUssd` exists, sets its response, persists via
    `App.INSTANCE.ussdStorage.updateUssd`, clears pending. **Pending state is in memory only.**

### Accessibility — `UssdAccessibilityService`
Declared in the manifest with `BIND_ACCESSIBILITY_SERVICE` and configured by `res/xml/service_config.xml`:
`packageNames=com.android.phone`, `typeWindowStateChanged`, `canRetrieveWindowContent=true`,
`accessibilityFlags=flagReportViewIds`, `notificationTimeout=100`. The manifest declares it `exported=true`
(protected by `BIND_ACCESSIBILITY_SERVICE`).

On every matching window-state event it:
0. **returns immediately unless `ussdExecutor.hasPending()`** (a run started by this app is waiting), so
   unrelated `com.android.phone` dialogs are neither read nor dismissed,
1. gets `event.getSource()` (root node); returns if the event or node is `null`,
2. looks up nodes by view id `android:id/message` (reply text) and `android:id/button1` (dialog button),
3. returns if either list is `null`/empty or a node is `null` (i.e. not a USSD dialog),
4. returns if the message text is `null`; otherwise calls `ussdExecutor.setResponse(text)` and sends
   `EventBus.Event.USSD_RESULT_RECEIVED`,
5. performs `ACTION_CLICK` on the button to dismiss the system dialog.

### UI
- `SetupActivity` (`exported=false`) — onboarding/status screen with two rows (CALL_PHONE permission;
  accessibility service enabled), bound with `ActivitySetupBinding`. Tapping a row requests the permission /
  opens system accessibility settings. `onResume` re-evaluates; "Next" appears when both are OK and launches
  `UssdListActivity` (`CLEAR_TASK|NEW_TASK`).
- `UssdListActivity` (launcher, `exported=true`) — `onStart` redirects to `SetupActivity` if prerequisites are
  missing. Shows a `RecyclerView` + FAB (`ActivityUssdListBinding`). FAB opens an `AlertDialog` whose view is
  `DialogAddUssdBinding` (`dialog_add_ussd.xml`): an `inputType=phone` field plus **`*` and `#` buttons** that
  insert the character at the caret, because some keyboards' phone dialpad (Gboard) cannot type them. The dialog
  does not auto-focus the field. Row tap runs the code. It subscribes to `USSD_RESULT_RECEIVED` in `onResume`
  and unsubscribes in `onPause`, reloading the list on each event. The only helper inner class left is
  `UiEventsReactor` (click callbacks for the FAB, dialog OK and list rows); the former ButterKnife holder classes
  (`ActivityViews`, `DialogViews`, `Logic`) were flattened into `binding` / `dialogBinding` fields.
- **Theme and window insets** — `Theme.Ussd` (`Theme.Material3.DayNight.NoActionBar`, `res/values/themes.xml`)
  is set on `<application>`; no colours are defined by the app. Both activities call `EdgeToEdge.enable(this)`
  before `super.onCreate` and apply `systemBars | displayCutout` insets with `ViewCompat.setOnApplyWindowInsetsListener`:
  `SetupActivity` pads its root; `UssdListActivity` pads the `RecyclerView` (`clipToPadding=false`) and adds the
  insets to the FAB margins. Insets are then consumed.
- `UssdListPresenter` — thin: `addUssd(code)`, `loadUssdList()`, `runUssd(ussd)`. Pushes data to the view via
  `UssdViewModel.setDataAndInvalidateView(List<Ussd>)` (naming is "view contract", not an Android ViewModel).
- `UssdListAdapter` — `RecyclerView.Adapter` using **DataBinding** (`ussd_list_item.xml`, variable `ussd`,
  `@{ussd.response ?? @string/default_ussd_response}`); `notifyDataSetChanged()` on every update. Row click
  resolves position through `ItemViewPositionProvider`.

### Cross-component events — `utils/EventBus`
Wrapper over `LocalBroadcastManager` with a single event enum (`USSD_RESULT_RECEIVED`). One receiver per event
type is kept in an `EnumMap`. `LocalBroadcastManager` is deprecated in AndroidX.

## Runtime flow: "run a code and capture the reply"

```
user taps row ─► UssdListActivity.UiEventsReactor.onUssdItemClicked
   └► UssdListPresenter.runUssd ─► UssdRunner.run
        ├ CALL_PHONE missing? → silently return
        ├ pendingUssd = clone
        └ startActivity(ACTION_CALL tel:*100%23)         ──► system dialer/telephony
                                                              │ USSD reply dialog appears
UssdAccessibilityService.onAccessibilityEvent  ◄──────────────┘ (package com.android.phone)
   ├ return unless UssdRunner.hasPending()  (no run pending → ignore the dialog)
   ├ find android:id/message + android:id/button1
   ├ UssdRunner.setResponse(text) ─► UssdSimpleStorage.updateUssd (SharedPreferences)
   ├ EventBus.sendEvent(USSD_RESULT_RECEIVED) ─► UssdListActivity reloads list (if resumed)
   └ click button1 (dismiss dialog)
```

## Persistence

| What | Where | Format |
|---|---|---|
| USSD entries | SharedPreferences file `SimpleStorage` | key `Ussd.id` → Base64(Java-serialized `Ussd`, pinned `serialVersionUID`) |
| Anything else | — | nothing (no DB, no files, no remote) |

## Threading / lifecycle notes
- All work is synchronous on the main thread (SharedPreferences `apply()` is async-on-disk but reads are sync).
- Accessibility callbacks arrive on the main thread; `App.INSTANCE` must be non-null (it is, same process).
- Process death between `run()` and the accessibility callback drops the reply (`pendingUssd` is lost); because
  the service now requires `hasPending()`, the dialog is then not auto-dismissed either.
- No configuration-change handling beyond defaults; state is reloaded in `onResume`.

## Build-time architecture
- Gradle 9.6.1 (`-bin`, sha256 pinned), AGP 9.5.1 via the plugins DSL, `settings.gradle` with `google()` +
  `mavenCentral()`; `gradle.properties` enables build cache and configuration cache. Run with JDK 17+ (25 verified).
- `compileSdk`/`targetSdk` 36, `minSdk` 23, Java 17 source/target (lambdas used heavily: `Predicate`, click
  listeners, `EventBus.Subscriber`). `namespace`/`applicationId` = `com.tooploox.ussd`. `versionCode 3` / `0.3`.
- `buildFeatures { viewBinding = true; dataBinding = true }`: ViewBinding classes for activities/dialog, DataBinding
  for `ussd_list_item.xml` (`BR.ussd`, used via `DataBindingUtil` in `UssdListAdapter`). No annotation processors.
- Release: `minifyEnabled = true`, `shrinkResources = false`, `proguard-android-optimize.txt` +
  `proguard-rules.pro` (keeps `Ussd`). R8 optimizes and obfuscates; it inlines `UssdRunner` into its callers, which is
  expected. Signing config is applied only when keystore and password are available.
- Lint: `abortOnError = true` with `lint-baseline.xml` (10 deferred findings, see [context.md](context.md)).
- CI: `.github/workflows/build.yml` runs `./gradlew assembleDebug lintDebug` on push/PR (not yet run on GitHub).
- See [upgrade docs](upgrade/README.md) for the migration analysis and status.

## Extension points / where to change things

| Goal | Touch |
|---|---|
| Add DI | Replace `App.INSTANCE.*` accesses in `UssdListActivity`, `UssdAccessibilityService`, `UssdRunner` |
| Swap storage (Room/DataStore) | Implement `UssdRepository`; mind migration from serialized prefs |
| Different way of getting replies | Implement `UssdExecutor`; see `TelephonyManager.sendUssdRequest` in [04-platform-behavior](upgrade/04-platform-behavior.md) |
| Support more OEM dialogs | `UssdAccessibilityService` constants + `service_config.xml` `packageNames` |
| Add tests | Presenter, `Strings`, `UssdRuntimeStorage`, `UssdSimpleStorage.ORDER` (comparator is package-private) are pure-Java testable today |
| Delete a saved code | Add UI that calls `UssdRepository.removeUssd` (implemented, unused) |
