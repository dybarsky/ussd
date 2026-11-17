# 03 — Build script migration

> **Status: done** — build script in d0bdd1c; R8 in "Switch to R8 optimize defaults with keep rule for Ussd"
> (4513802); lint in "Enable lint enforcement with baseline" (0e195f3). The sample in section 3 now matches the
> committed `build.gradle` except for comments. Sections 1, 6 and 7 keep the original analysis; "Current" means the
> pre-upgrade state (tag `0.2`).

Concrete before/after for `build.gradle`, `gradle.properties`, signing, R8/ProGuard and lint. Groovy DSL is kept
to minimise the diff; a Kotlin-DSL + version-catalog variant is noted at the end.

## 1. Original `build.gradle` (tag `0.2`) — problems line by line

| Current | Problem | Fix |
|---|---|---|
| `buildscript { … classpath 'com.android.tools.build:gradle:3.0.1' }` + `apply plugin:` | Legacy plugin application; AGP 3.0.1 can't run on Gradle 9 | `plugins { id 'com.android.application' version '9.5.1' }` |
| `repositories { google(); jcenter() }` in build file | JCenter dead; repos should be central | Move to `settings.gradle` (`google()`, `mavenCentral()`) |
| `implementation 'com.android.support:…:27.0.1'` | Support Library frozen | AndroidX, see [02](02-dependencies.md) |
| `annotationProcessor 'butterknife-compiler'` | ButterKnife removed | delete |
| `compileSdkVersion 27` / `buildToolsVersion "27.0.1"` | Old; `buildToolsVersion` unnecessary | `compileSdk = 36`; remove build tools |
| `dataBinding { enabled = true }` | Removed in favour of `buildFeatures` | `buildFeatures { dataBinding = true }` |
| `sourceCompatibility JavaVersion.VERSION_1_8` | OK but dated | `VERSION_17` |
| `signingConfigs.release { storePassword keystorePassword … }` | Fails with "Could not get unknown property" if the property is missing; secrets handling | Read from Gradle property **or** env, only configure if present |
| `minifyEnabled true`, `shrinkResources false`, `proguardFile 'proguard-android.txt'` | Old method-call style; `proguard-android.txt` copy disables optimisation/obfuscation | Property assignment; `getDefaultProguardFile('proguard-android-optimize.txt')` + `proguard-rules.pro` |
| `defaultConfig { versionCode 2; minSdkVersion 16; targetSdkVersion 27 }` | Method-call style, no `applicationId`/`namespace` | Assignment style, `namespace`, `applicationId`, `minSdk = 23`, `targetSdk = 36` |
| `lintOptions { abortOnError false }` | `lintOptions` removed | `lint { abortOnError = false }` (or better: a baseline) |
| `AndroidManifest.xml` `package="com.tooploox.ussd"` | AGP 8+ errors if `package` is the only namespace source | `namespace = 'com.tooploox.ussd'`; remove `package` from manifest |

## 2. Target `settings.gradle`
See [01-build-tooling.md §5](01-build-tooling.md#5-settingsgradle-new-required).

## 3. Target `build.gradle` (flat layout, Groovy)
Differences between the first draft of this sample and the committed file: release uses
`proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'` (as drafted), and
the `lint {}` block now sets `baseline = file('lint-baseline.xml')` with `abortOnError = true` (the draft had
`abortOnError = false` as a temporary step). The two explanatory comments on `minifyEnabled`/`shrinkResources`
were dropped. Below is the committed version.

```groovy
plugins {
    id 'com.android.application' version '9.5.1'
}

android {
    namespace = 'com.tooploox.ussd'
    compileSdk = 36

    defaultConfig {
        applicationId = 'com.tooploox.ussd'   // MUST stay identical to the published app
        minSdk = 23
        targetSdk = 36
        versionCode = 3                       // > 2, the last released value
        versionName = '0.3'
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        release {
            def ksPassword = providers.gradleProperty('keystorePassword')
                    .orElse(providers.environmentVariable('KEYSTORE_PASSWORD')).getOrNull()
            def ksFile = file(providers.environmentVariable('KEYSTORE_FILE')
                    .orElse(System.getProperty('user.home') + '/.android/dmax.keystore').get())
            if (ksPassword != null && ksFile.exists()) {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = 'dmax'
                keyPassword = ksPassword
            }
        }
    }

    buildTypes {
        release {
            // Only sign when credentials are available (CI debug builds and forks keep working)
            if (signingConfigs.release.storeFile != null) {
                signingConfig = signingConfigs.release
            }
            minifyEnabled = true
            shrinkResources = false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    lint {
        baseline = file('lint-baseline.xml')   // pre-existing findings; new issues fail the build
        abortOnError = true
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.8.0'
    implementation 'com.google.android.material:material:1.14.0'
    implementation 'androidx.recyclerview:recyclerview:1.4.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.2'
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'
}
```
Notes:
- Do **not** add `repositories {}` here (settings uses `FAIL_ON_PROJECT_REPOS`).
- `versionCode`/`versionName`: bump above the last published build before shipping.
- Verify the exact Groovy property names against deprecation warnings during the first sync — AGP 9.x is still
  moving `minifyEnabled`/`shrinkResources` toward the new `optimization {}` block (AGP 9.3+ offers
  `optimization { enable = true }`). Warnings are fine; errors mean rename. The committed file still uses
  `minifyEnabled`/`shrinkResources`; whether AGP 9.5.1 prints a deprecation warning for them was not checked here.
- `shrinkResources = false` is intentionally kept; lint reports it as `NotShrinkingResources` (baselined).
- Once DataBinding is the only generated-binding feature you still need, you may drop `viewBinding` and use
  `UssdListItemBinding` + view binding only for the activity layouts — both can coexist.

## 4. `gradle.properties`
Original content was only `keystorePassword=` (an empty committed placeholder). **Done (d0bdd1c):** removed;
secrets belong in `~/.gradle/gradle.properties` or CI env vars. The committed file is exactly:
```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
# android.useAndroidX is true by default in AGP 9 (explicit setting optional)
```
Do not add: `android.enableJetifier`, `android.nonFinalResIds=false`, `android.newDsl=false`,
`android.builtInKotlin=false`, `android.disallowKotlinSourceSets=false`.

## 5. Manifest namespace and application id
Status: done (d0bdd1c). Remove `package="com.tooploox.ussd"` from `src/main/AndroidManifest.xml` (it becomes `namespace`). Manifest changes
required by targetSdk are in [04](04-platform-behavior.md#1-manifest-fixes-blocking).

## 6. R8 / ProGuard
**Status: done (4513802).** `proguard-android.txt` was deleted (not renamed; its content was dropped) and
`proguard-rules.pro` was added with the two `Ussd` rules below plus an explanatory comment (the "Accessibility
service + data binding…" comment line from the draft is not in the file). R8 now optimizes and obfuscates the
release build, and inlines `UssdRunner` into its callers (normal). Release behaviour with data from the real old
APK is **not verified**. Original analysis follows.

The original `proguard-android.txt` was a 2017 copy of the SDK default with extra lines:
`-dontoptimize`, `-dontpreverify`, `-dontobfuscate`, `-dontwarn com.tooploox.ussd.ui.**`, `-dontwarn android.support.**`.

Recommended replacement `proguard-rules.pro` (and `git mv proguard-android.txt proguard-rules.pro`, since the
`-optimize` SDK default is now included separately):
```proguard
# Persisted data: Ussd is Java-serialized into SharedPreferences. Keep its name and members
# so existing installs can still deserialize it (default serialVersionUID depends on the class shape).
-keep class com.tooploox.ussd.domain.Ussd { *; }
-keepnames class com.tooploox.ussd.domain.Ussd

# Accessibility service + data binding are referenced from manifest/XML and are kept by AAPT2 rules automatically.
```
Other points:
- With `proguard-android-optimize.txt`, R8 will **optimize and obfuscate** (previously both were off). Because
  `Ussd` is Java-serialized, test the release build against data written by the previous release (see
  [05](05-plan-and-verification.md)). If in doubt, keep `-dontobfuscate` initially and re-enable later.
- AGP 9 sets `android.proguard.failOnMissingFiles=true`: a typo in a `proguardFiles` path now fails the build.
- AGP 9 ignores global options (`-dontoptimize`, `-dontobfuscate`) coming from **library** consumer rules, but
  honours them from the app module's own rules — which is where they live here, so no change is needed.
- Drop the `-dontwarn android.support.**`, `com.google.vending.licensing` rules and the default-copy boilerplate: they
  are obsolete.
- `strictFullModeForKeepRules` (AGP 9 default): `-keep class X` no longer keeps the default constructor. Nothing in
  this app is instantiated reflectively by name except via manifest/XML (handled by AAPT2).
- Enable `shrinkResources = true` only after confirming nothing is looked up by name (`getIdentifier`) — nothing is.

## 7. Lint
**Status: done (0e195f3).** `lint-baseline.xml` (generated by lint 9.5.1) holds 10 deferred findings, listed in
[context.md](../context.md#deferred-lint-findings-lint-baselinexml-generated-by-lint-951); `abortOnError = true`;
`lintDebug` passes. Cheap findings were fixed instead of baselined (dialog constraints/labels, FAB content description,
setup icons' accessibility, two unused resources). CI runs `lintDebug`. Original analysis:

`abortOnError false` hides everything. After the migration run `./gradlew lint`, then generate a baseline with
`lint { baseline = file('lint-baseline.xml') }` and flip `abortOnError` back to `true`. Expect new findings:
`ExportedService`, `MissingPermission` (already suppressed in `UssdRunner`), `UnusedResources`,
`RtlHardcoded`/`RtlSymmetry`, `DeprecatedApi`, `Overdraw`, `MissingApplicationIcon`/`IconLauncherShape` (no adaptive icon).

## 8. Optional: Kotlin DSL + version catalog (Phase 5)
`gradle/libs.versions.toml`:
```toml
[versions]
agp = "9.5.1"
appcompat = "1.8.0"
material = "1.14.0"
recyclerview = "1.4.0"
constraintlayout = "2.2.2"

[libraries]
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
material = { module = "com.google.android.material:material", version.ref = "material" }
androidx-recyclerview = { module = "androidx.recyclerview:recyclerview", version.ref = "recyclerview" }
androidx-constraintlayout = { module = "androidx.constraintlayout:constraintlayout", version.ref = "constraintlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```
Rename `build.gradle` → `build.gradle.kts` and `settings.gradle` → `settings.gradle.kts`; use `compileSdk = 36`,
`isMinifyEnabled = true`, `isShrinkResources = false`, `alias(libs.plugins.android.application)`.
