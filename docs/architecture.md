# Architecture

A small, hand-wired **MVP-ish** app. There is no DI framework, no Jetpack ViewModel/Room/Navigation, no
coroutines/RxJava. Everything runs on the main thread except the accessibility callback (also main thread).

## Layers and packages (`com.tooploox.ussd`)

```
┌──────────────────────────── ui ─────────────────────────────┐
│ UssdListActivity (launcher)  SetupActivity                  │
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
- `UssdRepository`: `addUssd / removeUssd / updateUssd / getUssdList`.
  - `UssdSimpleStorage` — one SharedPreferences file `SimpleStorage`; **key = `Ussd.id`, value = Base64 of
    Java-serialized `Ussd`** (via `ui/Serializer`, which oddly lives in the `ui` package). `updateUssd` = `addUssd`.
    `getUssdList` iterates `prefs.getAll()` (HashMap order → no stable ordering).
  - `UssdRuntimeStorage` — in-memory list; not referenced by production code (left over from early dev/tests).
- `UssdExecutor`: `run(Ussd)` and `setResponse(String)`.
  - `UssdRunner.run`: bails out if `CALL_PHONE` is not granted; stores `pendingUssd = ussd.clone()`; builds
    `tel:` URI with `#` percent-encoded; fires `Intent.ACTION_CALL` with `FLAG_ACTIVITY_NEW_TASK`.
  - `UssdRunner.setResponse`: if a `pendingUssd` exists, sets its response, persists via
    `App.INSTANCE.ussdStorage.updateUssd`, clears pending. **Pending state is in memory only.**

### Accessibility — `UssdAccessibilityService`
Declared in the manifest with `BIND_ACCESSIBILITY_SERVICE` and configured by `res/xml/service_config.xml`:
`packageNames=com.android.phone`, `typeWindowStateChanged`, `canRetrieveWindowContent=true`.

On every matching window-state event it:
1. gets `event.getSource()` (root node),
2. looks up nodes by view id `android:id/message` (reply text) and `android:id/button1` (dialog button),
3. returns if either is missing (i.e. not a USSD dialog),
4. calls `ussdExecutor.setResponse(text)`, sends `EventBus.Event.USSD_RESULT_RECEIVED`,
5. performs `ACTION_CLICK` on the button to dismiss the system dialog.

### UI
- `SetupActivity` — onboarding/status screen with two rows (CALL_PHONE permission; accessibility service
  enabled). Tapping a row requests the permission / opens system accessibility settings. `onResume`
  re-evaluates; "Next" appears when both are OK and launches `UssdListActivity` (`CLEAR_TASK|NEW_TASK`).
- `UssdListActivity` (launcher) — `onStart` redirects to `SetupActivity` if prerequisites are missing.
  Shows a `RecyclerView` + FAB. FAB opens an `AlertDialog` (`dialog_add_ussd.xml`) to enter a code; row tap
  runs it. It subscribes to `USSD_RESULT_RECEIVED` in `onResume` and unsubscribes in `onPause`, reloading the
  list on each event. Uses several inner "holder" classes (`ActivityViews`, `DialogViews`, `Logic`,
  `UiEventsReactor`) as ButterKnife targets — ButterKnife binds to those objects, not the activity.
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
   ├ find android:id/message + android:id/button1
   ├ UssdRunner.setResponse(text) ─► UssdSimpleStorage.updateUssd (SharedPreferences)
   ├ EventBus.sendEvent(USSD_RESULT_RECEIVED) ─► UssdListActivity reloads list (if resumed)
   └ click button1 (dismiss dialog)
```

## Persistence

| What | Where | Format |
|---|---|---|
| USSD entries | SharedPreferences file `SimpleStorage` | key `Ussd.id` → Base64(Java-serialized `Ussd`) |
| Anything else | — | nothing (no DB, no files, no remote) |

## Threading / lifecycle notes
- All work is synchronous on the main thread (SharedPreferences `apply()` is async-on-disk but reads are sync).
- Accessibility callbacks arrive on the main thread; `App.INSTANCE` must be non-null (it is, same process).
- Process death between `run()` and the accessibility callback drops the reply (`pendingUssd` is lost).
- No configuration-change handling beyond defaults; state is reloaded in `onResume`.

## Build-time architecture
- Java 8 source/target (lambdas used heavily: `Predicate`, click listeners, `EventBus.Subscriber`).
- DataBinding enabled (`BR.ussd`, generated binding classes); ButterKnife annotation processing.
- R8 minification on release with custom `proguard-android.txt` that disables optimization and obfuscation.
- See [upgrade docs](upgrade/README.md) for what changes in the target toolchain.

## Extension points / where to change things

| Goal | Touch |
|---|---|
| Add DI | Replace `App.INSTANCE.*` accesses in `UssdListActivity`, `UssdAccessibilityService`, `UssdRunner` |
| Swap storage (Room/DataStore) | Implement `UssdRepository`; mind migration from serialized prefs |
| Different way of getting replies | Implement `UssdExecutor`; see `TelephonyManager.sendUssdRequest` in [04-platform-behavior](upgrade/04-platform-behavior.md) |
| Support more OEM dialogs | `UssdAccessibilityService` constants + `service_config.xml` `packageNames` |
| Add tests | Presenter, `Strings`, `UssdRuntimeStorage` are pure-Java testable today |
