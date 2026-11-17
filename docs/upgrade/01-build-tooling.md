# 01 — Build tooling: Gradle, JDK, AGP, SDK, CI

## 1. Current state
- `gradle/wrapper/gradle-wrapper.properties` → `gradle-4.2-all.zip` (the `-all` distribution; wrapper date 2017-03-01)
- `build.gradle`: `buildscript { classpath 'com.android.tools.build:gradle:3.0.1' }` + `apply plugin: 'com.android.application'`
- **No `settings.gradle`**. Gradle 4.x tolerated this; Gradle 9 does not support builds without a settings file.
- Repos: `google()` + `jcenter()` (JCenter was sunset in 2021–22; treat as unavailable).
- Travis CI config pins `oraclejdk8`, `build-tools-27.0.1`, `android-27`.
- Local machine: JDK 25, SDK platforms 36 / 37.0, build-tools 36.0.0 / 37.0.0, no platform 27, Gradle 9.6.1 cached.

## 2. Target

| Tool | Version | Notes |
|---|---|---|
| AGP | **9.5.1** | Latest stable on 2026-11-17. Supports up to API 37. |
| Gradle | **9.6.1 or newer** | AGP 9.5 minimum = 9.6.0. Latest = 9.9.1. Use a `-bin` distribution and add `distributionSha256Sum`. |
| JDK | **17 minimum**, 21 recommended | Android Studio's bundled JBR is fine. JDK 25 works with AGP 9.5.0+. Set Studio's *Gradle JDK* accordingly; for CLI set `JAVA_HOME` or `org.gradle.java.home`. |
| Build-Tools | 36.0.0 (implicit) | Remove `buildToolsVersion`; AGP picks its default and auto-downloads if the license is accepted. |
| compileSdk | 36 | `platforms;android-36` (already installed locally). 37 is allowed. |
| NDK | not used | — |
| Android Studio | a release that supports AGP 9.5 | Check the AGP 9.5 notes for the matching Studio version before opening the project. |

Compatibility source: <https://developer.android.com/build/releases/gradle-plugin> (AGP 9.5 table: Gradle 9.6.0 /
Build-Tools 36.0.0 / JDK 17 / max API 37).

## 3. Why a single jump (not an AGP ladder)
Every AGP major from 4 to 9 removed APIs (old DSL names, `jcenter`, `applicationVariants`, non-namespaced manifests,
final R fields, Jetifier defaults…). The build has no custom plugins, flavors, variants API use, or tasks, so the
only things to rewrite are the ~10 DSL lines. The AGP Upgrade Assistant in Android Studio cannot start from 3.0.1
anyway (it needs a project that syncs). Hand-write the new files from [03-build-script.md](03-build-script.md).

## 4. Bootstrapping the wrapper (chicken-and-egg)
The old Gradle 4.2 runtime cannot execute on a modern JDK, but the **wrapper JAR only downloads and launches** the
configured distribution, so:

```bash
# 1. Edit gradle/wrapper/gradle-wrapper.properties by hand:
#    distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
# 2. Add the new settings.gradle and build.gradle (see 03-build-script.md)
# 3. Regenerate wrapper scripts/jar with the new Gradle (keeps files consistent, adds checksum):
JAVA_HOME=<jdk17-or-21> ./gradlew wrapper --gradle-version 9.6.1 --distribution-type bin \
    --gradle-distribution-sha256-sum <sha256 from https://services.gradle.org/distributions/gradle-9.6.1-bin.zip.sha256>
# 4. Smoke tests
./gradlew help
./gradlew build --dry-run
```
If the old wrapper JAR refuses to start on your JDK, run a system Gradle once (`brew install gradle`, or use the
cached `~/.gradle/wrapper/dists/gradle-9.6.1-bin/*/gradle-9.6.1/bin/gradle wrapper …`).

`gradlew` / `gradlew.bat` are from 2017 and will be replaced by the `wrapper` task; commit all four wrapper files.

## 5. `settings.gradle` (new, required)
```groovy
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ussd"
```
- Replace `jcenter()` with `mavenCentral()`. (ButterKnife 8.7.0 on JCenter is also on Central, but it is being removed.)
- `FAIL_ON_PROJECT_REPOS` means the app build file must not declare its own `repositories {}`.
- If you later move sources into `app/`, add `include(":app")`.

## 6. Project layout decision
Flat layout (sources at repo root) still works with AGP 9, but is non-standard: the root project *is* the app, so
the plugin version must be declared inline and tools/IDE templates assume `app/`. **Recommended (Phase 5, optional)**:
`git mv src app/src`, `git mv proguard-android.txt app/proguard-rules.pro`, create `app/build.gradle` and a slim root
`build.gradle`. Do it as a separate commit after the build is green so history stays readable.

## 7. SDK / environment setup
```bash
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
sdkmanager --licenses
```
Create `local.properties` (git-ignored) with `sdk.dir=/Users/<you>/Library/Android/sdk` or export `ANDROID_HOME`.
Remove the Travis license hacks — they are not needed with GitHub-hosted runners.

## 8. CI — replace Travis with GitHub Actions
`.github/workflows/build.yml`:
```yaml
name: build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew assembleDebug lint
      - uses: actions/upload-artifact@v4
        with: { name: apk, path: build/outputs/apk/debug/*.apk }   # app/build/... after the app/ move
```
(Pin action versions to whatever is current when you implement it. Ubuntu runners ship an Android SDK; if
`platforms;android-36` is missing, add `android-actions/setup-android`.) Delete `.travis.yml` in the same commit.
Release signing in CI: base64-encode the keystore into a secret and read passwords from env vars (see 03).

## 9. Gradle settings worth enabling (`gradle.properties`)
```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
```
`android.useAndroidX` is **already `true` by default in AGP 9**, so the flag is optional. Do **not** add
`android.enableJetifier=true`: no dependency remains that needs it once the Support Library is gone.
Remove migration flags AGP 9 tells you to drop (`android.builtInKotlin`, `android.newDsl`,
`android.uniquePackageNames`, `android.enableAppCompileTimeRClass`) if you ever added them.

## 10. AGP 9 changes that matter *for this project*
Checked against the AGP 9.0.0 release notes (the 9.x line keeps them):

| AGP 9 change | Impact here |
|---|---|
| New DSL interfaces only; legacy `BaseExtension` & variant API removed | None — no custom variant code. Use property-assignment syntax (`compileSdk = 36`). |
| Built-in Kotlin default on | None — pure Java. No Kotlin plugin should be applied. |
| `android.useAndroidX` default true | Good — matches the target. |
| `android.sdk.defaultTargetSdkToCompileSdkIfUnset` | Set `targetSdk` explicitly anyway. |
| `android.proguard.failOnMissingFiles=true` | `proguardFile 'proguard-android.txt'` exists, so OK; keep the filename correct when renaming. |
| `getDefaultProguardFile('proguard-android.txt')` rejected (only `-optimize` variant) | Project uses its **own copy** at the root, not the SDK default, so no build break — but switch to `proguard-android-optimize.txt` + a small custom file (see 03). |
| `android.r8.strictFullModeForKeepRules=true` | `-keep class X` no longer keeps the default constructor. Our rules rely on `-keep` for ILicensingService only; verify release build works. |
| `android.r8.optimizedResourceShrinking=true`, resource shrinking always inside R8 | We have `shrinkResources false`; can be enabled afterwards. |
| `android.defaults.buildfeatures.resvalues=false`, `shaders=false` | No `resValue`/shaders used. |
| `android.dependency.useConstraints=false`, `onlyEnableUnitTestForTheTestedBuildType=true` | No effect (no tests yet). |
| Java source/target default → 11 | We set it explicitly. |
| Min Gradle 9.1 (9.0) → **9.6** (AGP 9.5) | Use Gradle ≥ 9.6.0. |
| `android.newDsl=false` opt-out exists but is **removed in AGP 10** | Don't use it. |

## 11. Verification (tooling phase)
- `./gradlew --version` shows Gradle ≥ 9.6 on JDK ≥ 17.
- `./gradlew help` and `./gradlew build --dry-run` succeed (do not use `clean` as a check; it proves nothing).
- `./gradlew assembleDebug` compiles after Phase 2 (AndroidX) is also done. Until then the app sources will not
  compile against AGP 9 — that is expected.
