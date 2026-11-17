# AGENTS.md — USSD ("Accessibility demo")

Context file for AI coding agents (and humans). Read this first, then follow the links.

## What this app is

A tiny Android app that lets the user save USSD / "quick codes" (e.g. `*100#`), tap one to dial it, and then
**reads the operator's reply out of the system USSD dialog using an `AccessibilityService`**, stores it next to
the code and dismisses the dialog. Launcher label: "Accessibility demo". Open source (MIT), single author.

- Package / applicationId: `com.tooploox.ussd` (**must never change** — it is the app's identity on Google Play)
- Language: Java 8, no Kotlin
- Version: `versionCode 2` / `versionName "0.2"`
- Single Gradle module; **the app module is the repo root** (no `app/` directory, no `settings.gradle`)
- No tests, no DI framework, no network access (no `INTERNET` permission)

## Repo map

```
build.gradle              single build file (buildscript + app module together)
gradle.properties         only `keystorePassword=` (placeholder, see docs/upgrade)
gradle/wrapper/           Gradle 4.2 wrapper
proguard-android.txt      custom copy of the old SDK default rules (dontoptimize / dontobfuscate)
.travis.yml               dead CI config (travis-ci.org is gone)
POLICY.md                 privacy policy (generic template, see "Known quirks")
src/main/AndroidManifest.xml
src/main/java/com/tooploox/ussd/
    App.java                       Application; hand-rolled service locator (App.INSTANCE)
    accessibility/                 UssdAccessibilityService  (reads the system dialog)
    data/                          UssdRepository, UssdExecutor + implementations
    domain/Ussd.java               the only model class
    ui/                            activities, adapter, presenter, Serializer
    utils/                         EventBus, SetupHelper, Strings, Predicate, ItemViewPositionProvider
src/main/res/                      layouts, drawables, strings (English only)
```

## Documentation index

| Doc | Purpose |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Layers, classes, runtime flow, threading, persistence |
| [docs/context.md](docs/context.md) | Product/domain context, permissions, Android-specific gotchas, known quirks |
| [docs/upgrade/README.md](docs/upgrade/README.md) | **Modernization roadmap** — current vs target versions, phases, effort |
| [docs/upgrade/01-build-tooling.md](docs/upgrade/01-build-tooling.md) | Gradle, JDK, AGP, SDK, settings.gradle, wrapper bootstrap, CI |
| [docs/upgrade/02-dependencies.md](docs/upgrade/02-dependencies.md) | Support Library → AndroidX, Material, ButterKnife removal, import/XML mapping tables |
| [docs/upgrade/03-build-script.md](docs/upgrade/03-build-script.md) | Old → new `build.gradle`, `gradle.properties`, signing, R8/ProGuard, lint |
| [docs/upgrade/04-platform-behavior.md](docs/upgrade/04-platform-behavior.md) | targetSdk 36 behaviour changes, manifest, edge-to-edge, accessibility/USSD API risks, Play policy |
| [docs/upgrade/05-plan-and-verification.md](docs/upgrade/05-plan-and-verification.md) | Ordered step-by-step plan, verification checklist, risk register |

## Current state in one table (as of 2026-11-17)

| Item | Now | Target (see docs/upgrade) |
|---|---|---|
| Android Gradle Plugin | 3.0.1 | 9.5.1 |
| Gradle | 4.2 | 9.6.1+ (9.9.1 is current) |
| JDK to run the build | 8 | 17 minimum, 21 recommended |
| compileSdk / targetSdk | 27 / 27 | 36 / 36 |
| minSdk | 16 | 23 (forced by current AndroidX/Material) |
| buildToolsVersion | 27.0.1 (explicit) | omit (AGP default 36.0.0) |
| UI libs | `com.android.support:*:27.0.1` | AndroidX + Material 1.14 |
| View binding | ButterKnife 8.7.0 + DataBinding | ViewBinding (+ existing DataBinding) |
| Repos | `jcenter()` (shut down) | `google()` + `mavenCentral()` |
| CI | Travis | GitHub Actions |

## Working conventions for agents

- **The code currently cannot be built with a modern toolchain.** Gradle 4.2/AGP 3.0.1 do not run on JDK 17+
  and Android platform 27 is not installed locally. Do not try to "just run `./gradlew assembleDebug`" before
  the Phase 1 tooling migration in [docs/upgrade](docs/upgrade/README.md).
- Do the migration as a **rewrite of the build files**, not as an incremental AGP 3 → 4 → … → 9 ladder. The
  build script is ~60 lines; a ladder costs far more than it saves.
- Keep `applicationId`/`namespace` = `com.tooploox.ussd` and keep the class `com.tooploox.ussd.domain.Ussd`
  (fully-qualified name and fields) unchanged: user data is stored as Java-serialized objects in
  SharedPreferences and is keyed to that class (see [docs/context.md](docs/context.md#persistence-gotcha)).
- Never commit signing secrets. Release signing expects `~/.android/dmax.keystore` (not in repo).
- Local environment facts (checked 2026-11-17): JDK 25 (Android Studio JBR/JDK 17–21 is safer), Android SDK at
  `~/Library/Android/sdk` with platforms `android-36`, `android-37.0` and build-tools `36.0.0`, `37.0.0`;
  Gradle 9.6.1 / 9.1.0 / 8.10.2 distributions already cached in `~/.gradle/wrapper/dists`.
- Code style in repo: 4-space indent, Java, lambdas allowed (Java 8), file header comments with author/date.
- When you change architecture, update [docs/architecture.md](docs/architecture.md). When you complete a
  migration phase, tick it off in [docs/upgrade/05-plan-and-verification.md](docs/upgrade/05-plan-and-verification.md).

## Known quirks (short list — details in docs/context.md)

1. Core feature depends on an OEM/system dialog (`com.android.phone`, view ids `android:id/message` and
   `android:id/button1`) — fragile across Android versions and vendors.
2. `UssdAccessibilityService.onAccessibilityEvent` can NPE (`event.getSource()` may be null).
3. Persistence is Base64(Java serialization) in SharedPreferences, no `serialVersionUID`, list order unstable.
4. Manifest lacks `android:exported` (build error once targetSdk ≥ 31).
5. `POLICY.md` describes data collection (IP address, location, analytics) the app does not actually perform.
6. Stray duplicate directory `src/main/res/res/` (unused copies of two drawables).
