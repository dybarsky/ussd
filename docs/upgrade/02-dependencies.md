# 02 — Dependencies and library migration

> **Status: done.** Imports: "Migrate Java imports from Support Library to AndroidX" (6b338ae). Layouts/theme:
> "Migrate layouts and theme to AndroidX and Material 3" (6ec2a46). ButterKnife → ViewBinding: "Replace ButterKnife
> with ViewBinding" (16e6304). Dependencies block: d0bdd1c. The repo's `dependencies {}` is identical to the
> block in section 3. `LocalBroadcastManager` was kept (section 6, option 1).

Versions verified against Google Maven / Maven Central on 2026-11-17. Tables in sections 1, 4 and 5 describe the
**original** (pre-upgrade) state and the mapping that was applied.

## 1. Inventory

| Original coordinate | Role in the app | Target (now in the repo) |
|---|---|---|
| `com.android.support:support-v4:27.0.1` | `ActivityCompat`, `LocalBroadcastManager` | `androidx.core:core` (transitive via appcompat) + `androidx.localbroadcastmanager:localbroadcastmanager:1.1.0` (or remove) |
| `com.android.support:appcompat-v7:27.0.1` | `AppCompatActivity`, `AlertDialog`, themes | `androidx.appcompat:appcompat:1.8.0` |
| `com.android.support:recyclerview-v7:27.0.1` | `RecyclerView`, `LinearLayoutManager` | `androidx.recyclerview:recyclerview:1.4.0` |
| `com.android.support:design:27.0.1` | `CoordinatorLayout`, `FloatingActionButton` | `com.google.android.material:material:1.14.0` (brings `coordinatorlayout`) |
| `com.android.support.constraint:constraint-layout:1.0.2` | `ConstraintLayout` in 3 layouts | `androidx.constraintlayout:constraintlayout:2.2.2` |
| `com.jakewharton:butterknife:8.7.0` (+ `butterknife-compiler` as `annotationProcessor`) | `@BindView`, `@OnClick` in 2 activities | **Remove** → ViewBinding |
| DataBinding (AGP built-in, `dataBinding { enabled = true }`) | `ussd_list_item.xml` → `UssdListAdapter` | Keep; `buildFeatures { dataBinding = true }`; `androidx.databinding.*` imports |

No test dependencies exist. No Kotlin. No networking/JSON/DB libraries.

## 2. minSdk consequence (important)
Unpacked AAR manifests on 2026-11-17:

| Artifact | declared `minSdkVersion` |
|---|---|
| `appcompat:1.8.0` | **23** |
| `material:1.14.0` | **23** |
| `core:1.19.1` | **23** |
| `constraintlayout:2.2.2`, `recyclerview:1.4.0`, `coordinatorlayout:1.3.0` | 21 |
| `localbroadcastmanager:1.1.0`, `databinding-runtime` | 14 |

So with current libraries `minSdk` must be **≥ 23** (Android 6.0, which also guarantees runtime permissions and
removes the `findAccessibilityNodeInfosByViewId` API-18 lint suppression). Alternatives if API 16–22 support is
truly required: pin `appcompat:1.7.1` / older `material` / `core` (≥ 21 floor at best; API 16 is not reachable with any
maintained AndroidX line) — not recommended; maintenance cost outweighs the tiny device share.

## 3. Target dependency block (Groovy)
```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.8.0'
    implementation 'com.google.android.material:material:1.14.0'
    implementation 'androidx.recyclerview:recyclerview:1.4.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.2'
    // Only if you keep LocalBroadcastManager (deprecated); otherwise drop (see section 6)
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'
}
```
`androidx.core:core` and `coordinatorlayout` arrive transitively. DataBinding runtime is added by AGP when
`buildFeatures.dataBinding = true`; do not declare it manually. For Phase 5, prefer a version catalog
(`gradle/libs.versions.toml`).

## 4. Source / XML migration mapping (mechanical)

Android Studio's *Refactor → Migrate to AndroidX* needs a syncable project, so do it with search/replace
(`grep -rl` + `sed`) after the build files are rewritten.

### Java imports
| Old | New |
|---|---|
| `android.support.annotation.NonNull` / `Nullable` | `androidx.annotation.NonNull` / `Nullable` |
| `android.support.v7.app.AppCompatActivity` | `androidx.appcompat.app.AppCompatActivity` |
| `android.support.v7.app.AlertDialog` | `androidx.appcompat.app.AlertDialog` |
| `android.support.v7.widget.RecyclerView` | `androidx.recyclerview.widget.RecyclerView` |
| `android.support.v7.widget.LinearLayoutManager` | `androidx.recyclerview.widget.LinearLayoutManager` |
| `android.support.v4.app.ActivityCompat` | `androidx.core.app.ActivityCompat` |
| `android.support.v4.content.LocalBroadcastManager` | `androidx.localbroadcastmanager.content.LocalBroadcastManager` |
| `android.databinding.DataBindingUtil` | `androidx.databinding.DataBindingUtil` |
| `android.databinding.ViewDataBinding` | `androidx.databinding.ViewDataBinding` |
| `butterknife.*` | *(delete — replaced by ViewBinding)* |
| `com.tooploox.ussd.BR`, `com.tooploox.ussd.R` | unchanged (package comes from `namespace`) |

Files containing legacy imports: `SetupActivity`, `UssdListActivity`, `UssdListAdapter`, `Strings`, `EventBus`,
`SetupHelper`.

### Layout XML
| Old tag / attribute | New |
|---|---|
| `android.support.constraint.ConstraintLayout` (activity_setup, dialog_add_ussd, ussd_list_item) | `androidx.constraintlayout.widget.ConstraintLayout` |
| `android.support.design.widget.CoordinatorLayout` (activity_ussd_list) | `androidx.coordinatorlayout.widget.CoordinatorLayout` |
| `android.support.design.widget.FloatingActionButton` | `com.google.android.material.floatingactionbutton.FloatingActionButton` |
| `android.support.v7.widget.RecyclerView` | `androidx.recyclerview.widget.RecyclerView` |
| `@style/Theme.AppCompat.Light.NoActionBar` (manifest, both activities) | a Material 3 theme (see below) |
| `layout_marginLeft`, `layout_constraintLeft_*` | `layout_marginStart`, `layout_constraintStart_*` (RTL lint; optional) |
| `android:layout_alignParentStart/Left` on a RecyclerView inside CoordinatorLayout | delete (no effect there) |

Unchanged and still valid: `app:elevation`, `app:borderWidth`, `app:rippleColor` on the FAB,
`@style/TextAppearance.AppCompat.*`, `@style/Widget.AppCompat.Button.Colored`, data-binding expressions
(`@{ussd.response ?? @string/default_ussd_response}`).

### ⚠ Theme requirement for the Material FAB
`com.google.android.material` components throw at inflate time unless the activity theme descends from
`Theme.MaterialComponents.*`/`Theme.Material3.*` ("The style on this component requires your app theme to be
Theme.MaterialComponents (or a descendant)"). The manifest currently uses `Theme.AppCompat.Light.NoActionBar`.
Create `res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.Ussd" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- colorPrimary / colorSecondary overrides; app used ?attr/colorAccent -> map to colorSecondary -->
    </style>
</resources>
```
and set `android:theme="@style/Theme.Ussd"` on `<application>` (instead of per-activity). **Done (6ec2a46):**
`themes.xml` defines `Theme.Ussd` with parent `Theme.Material3.DayNight.NoActionBar` and **no overrides** (the app
has no brand colours; the Material 3 default accent changed the look, see context.md quirk 16). Original text: Replace
`?attr/colorAccent` usages (`ussd_list_item.xml`, FAB `rippleColor`) with `?attr/colorSecondary`/`colorPrimary`
(Material 3 no longer defines `colorAccent` semantics). `Widget.AppCompat.Button.Colored` still resolves
(M3 themes inherit AppCompat), but `Widget.Material3.Button` is the natural replacement.

## 5. ButterKnife → ViewBinding
**Status: done (16e6304).** `buildFeatures { viewBinding = true; dataBinding = true }` is in `build.gradle`;
`SetupActivity` uses `ActivitySetupBinding`; `UssdListActivity` uses `ActivityUssdListBinding` and
`DialogAddUssdBinding`. Deviations from the sketch below: `UiEventsReactor` was **kept** as an inner class (it
holds the click callbacks; `ActivityViews`, `DialogViews`, `Logic` were removed), and `UssdListAdapter` still
uses `DataBindingUtil.inflate` + `setVariable(BR.ussd, …)` rather than `UssdListItemBinding` (the optional
change was not made). The rest of this section is the original analysis.

ButterKnife is archived/deprecated. Its annotations need `R.id.*` to be compile-time constants; AGP 8+ generates
**non-final** `R` fields by default (AGP 9 enforces app compile-time non-final R), so `@BindView(R.id.x)` fails
with "attribute value must be a constant". Stopgap `android.nonFinalResIds=false` is possible on AGP 8 but is
being phased out — **do not rely on it**; remove ButterKnife.

Usage in the project (small):

| File | Usage |
|---|---|
| `SetupActivity` | 5 × `@BindView`, `ButterKnife.bind(this)`; unused `@OnClick` import |
| `UssdListActivity` | `ActivityViews` (RecyclerView), `DialogViews` (EditText), `UiEventsReactor` (`@OnClick(R.id.button_add)`), `ButterKnife.bind` ×3 (activity, reactor, dialog) |

Migration sketch:
```groovy
android { buildFeatures { viewBinding = true; dataBinding = true } }
```
```java
// SetupActivity
private ActivitySetupBinding binding;
binding = ActivitySetupBinding.inflate(getLayoutInflater());
setContentView(binding.getRoot());
// binding.permissionsStatus, binding.accessibilityStatus, binding.permissionsDescription,
// binding.accessibilityDescription, binding.next
```
```java
// UssdListActivity
binding = ActivityUssdListBinding.inflate(getLayoutInflater());
binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
binding.buttonAdd.setOnClickListener(v -> showAddUssdDialog());
// dialog: DialogAddUssdBinding.inflate(getLayoutInflater()); new AlertDialog.Builder(this).setView(b.getRoot())
```
Binding class names are derived from layout file names (`activity_setup` → `ActivitySetupBinding`, fields from
ids in camelCase). `ussd_list_item.xml` is a `<layout>` (DataBinding) file and already generates
`UssdListItemBinding`; the adapter can use it directly instead of `DataBindingUtil.inflate` +
`setVariable(BR.ussd, …)`.

The inner holder classes (`ActivityViews`, `DialogViews`, `Logic`, `UiEventsReactor`) existed only to serve ButterKnife and
could be flattened (done, except `UiEventsReactor`, which was kept). If you prefer minimal diff, a temporary fallback is ButterKnife **10.2.3** (AndroidX) plus
`android.nonFinalResIds=false` on AGP ≤ 8 — **not available as a durable option on AGP 9**.

## 6. LocalBroadcastManager (deprecated)
Status: option 1 taken — `androidx.localbroadcastmanager:1.1.0` is still a dependency and `EventBus` is unchanged.
`EventBus` is a thin wrapper used for one event (`USSD_RESULT_RECEIVED`). It works unchanged with
`androidx.localbroadcastmanager:1.1.0`, but the artifact is deprecated and unmaintained. Options, cheapest first:
1. Keep it for the upgrade (zero risk), mark it with a TODO.
2. Replace with a plain in-process observer list (the service and activity share one process), or
3. `androidx.lifecycle` `LiveData`/`MutableLiveData<Long>` owned by `App`/repository, observed lifecycle-aware in
   `UssdListActivity` (removes the manual subscribe/unsubscribe in `onResume/onPause`).

## 7. Optional additions (not required to build)
- `androidx.activity:activity` (`enableEdgeToEdge()`; already transitive through appcompat — see 04).
- Test stack when you add tests: `junit:junit`, `androidx.test.ext:junit`, `androidx.test.espresso:espresso-core`,
  optionally Robolectric. The `Presenter`, `Strings` and `UssdRuntimeStorage` classes are already unit-testable.
- `androidx.lifecycle:lifecycle-viewmodel` if you move presenter state into a real `ViewModel`.

## 8. How to re-verify versions before executing
```bash
G=https://dl.google.com/dl/android/maven2
for p in com/android/tools/build/gradle androidx/appcompat/appcompat com/google/android/material/material \
         androidx/recyclerview/recyclerview androidx/constraintlayout/constraintlayout; do
  echo "$p: $(curl -s $G/$p/maven-metadata.xml | grep -o '<release>[^<]*')"
done
curl -s https://services.gradle.org/versions/current | grep '"version"'
```
Prefer the newest **stable** (no `-alpha/-beta/-rc`); `release` in the metadata can point at an alpha for some
artifacts (e.g. AGP showed `9.6.0-alpha04`, activity showed `1.14.0-alpha03`).
