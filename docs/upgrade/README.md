# Modernization roadmap

> **Status (2026-11-17): Phases 1–4 are implemented in code on branch `modernize/agp-9`; Phase 0 is partly done
> (UID pinned); Phase 5 not started.** Commits are listed in
> [05 — Execution log](05-plan-and-verification.md#execution-log). `assembleDebug` and `lintDebug` were re-run
> successfully when this status was written; `assembleRelease` passed per the message of commit 0e195f3 (not re-run
> for this note). Not done / not verified: upgrade-over-old-APK data test, real USSD round trip on a SIM device and
> OEM variants, first GitHub Actions run, Play Console steps, dark mode / large font / RTL / tablet visual checks.
> The analysis below is the pre-upgrade text, kept as reference; "Current"/"Original" columns mean the state at
> tag `0.2`.

Analysis date: **2026-11-17**. Versions below were checked against Google Maven, Maven Central, services.gradle.org
and developer.android.com on that date. Re-check before executing (`curl` the `maven-metadata.xml` files).

## TL;DR

(Written before the upgrade.) The project was frozen at the Dec 2017 toolchain (AGP 3.0.1 / Gradle 4.2 / API 27 /
Support Library 27.0.1). It was **9 major AGP versions** behind (3 → 9) and could not be built on any current JDK. Because the build is a single
~60-line `build.gradle` and ~25 Java files, the right strategy is a **one-shot rewrite of the build + a mechanical
AndroidX migration**, followed by targeted platform-behaviour fixes — not a step-by-step AGP ladder.

Estimated effort for one engineer who knows Android: **~1–2 days** for "builds, runs, passes lint on API 36";
**+1–2 days** if you also replace ButterKnife with ViewBinding, adopt Material 3 theming, edge-to-edge and add
CI/tests.

## Version matrix

| Component | Original (tag `0.2`) | Target — now in the repo unless noted | Why / source |
|---|---|---|---|
| Android Gradle Plugin | 3.0.1 | **9.5.1** (latest stable; 9.6.0-alpha04 exists, ignore) | Google Maven metadata |
| Gradle | 4.2 | **≥ 9.6.0 required by AGP 9.5**; use 9.6.1 (cached locally) or 9.9.1 (current) | AGP 9.5 compat table; services.gradle.org |
| JDK for Gradle | 8 | **17 min**, 21 recommended (25 works; a JDK 25 warning was fixed in AGP 9.5.0). Verified here with JDK 25 (Android Studio JBR); no JDK 17/21 installed locally | AGP 9.5 compat table |
| SDK Build-Tools | 27.0.1 pinned | **omit** — AGP 9.5 default/minimum is 36.0.0 | AGP 9.5 compat table |
| compileSdk | 27 | **36** (AGP 9.5 supports up to 37) | |
| targetSdk | 27 | **36** (37 optional) — check Play's current target-API deadline | Play policy changes yearly |
| minSdk | 16 | **23** | `appcompat 1.8.0`, `material 1.14.0`, `core 1.19.1` AARs declare `minSdkVersion=23` (verified by unpacking the AARs) |
| Java language level | 1.8 | 17 (11 is AGP 9's default; 8 still works) | |
| Kotlin | none | none needed (AGP 9 has built-in Kotlin if you add it later) | |
| `androidx.appcompat:appcompat` | `support appcompat-v7:27.0.1` | 1.8.0 | |
| `com.google.android.material:material` | `support design:27.0.1` | 1.14.0 | |
| `androidx.recyclerview:recyclerview` | `support recyclerview-v7:27.0.1` | 1.4.0 | |
| `androidx.constraintlayout:constraintlayout` | 1.0.2 | 2.2.2 | |
| `androidx.localbroadcastmanager` | in `support-v4` | 1.1.0 (deprecated — prefer removing) | |
| ButterKnife | 8.7.0 | **removed** → ViewBinding (done, 16e6304). Reason: archived; last release 10.2.3; relies on `R.id` constants which AGP ≥ 8 no longer generates | |
| DataBinding | `dataBinding { enabled = true }` | `buildFeatures { dataBinding = true }`, `androidx.databinding.*` | |
| CI | Travis (`.travis.yml`) | GitHub Actions (done, 727ad14; never run on GitHub yet) | travis-ci.org is shut down |

## Documents

1. [01-build-tooling.md](01-build-tooling.md) — Gradle wrapper bootstrap, JDK, SDK, `settings.gradle`, CI
2. [02-dependencies.md](02-dependencies.md) — library by library, AndroidX/Material mapping tables
3. [03-build-script.md](03-build-script.md) — before/after `build.gradle`, `gradle.properties`, signing, R8, lint
4. [04-platform-behavior.md](04-platform-behavior.md) — manifest, targetSdk 36 changes, edge-to-edge, accessibility, Play policy
5. [05-plan-and-verification.md](05-plan-and-verification.md) — ordered plan with checkboxes, risk register

## Phases at a glance

| Phase | Outcome | Main work |
|---|---|---|
| **0. Safety net** | Nothing lost on update | Pin `Ussd.serialVersionUID` from the last release; tag current commit; back up keystore |
| **1. Toolchain** | `./gradlew help` works on JDK 17/21 | `settings.gradle`, wrapper → Gradle 9.6.1+, AGP 9.5.1, new DSL, remove `buildToolsVersion`/`jcenter` |
| **2. AndroidX** | App compiles | Import + layout XML renames, drop ButterKnife (or move to 10.2.3 as stopgap), DataBinding package rename, minSdk 23 |
| **3. Platform** | App behaves on API 36 | `namespace`, `android:exported`, targetSdk 36, Material theme (FAB requires it), edge-to-edge, null-safety in service |
| **4. Hardening** | Shippable | R8 rules (`proguard-android-optimize.txt`), lint cleanup, GitHub Actions, signing via env, Play Accessibility declaration, fix `POLICY.md` |
| **5. Optional modernization** | Lower maintenance | Kotlin DSL + version catalog, move sources to `app/`, replace `LocalBroadcastManager`, ViewModel/LiveData, unit tests, `TelephonyManager.sendUssdRequest` |

Status per phase: **0** done except tag/keystore/old-APK items (UID pinned, 574984d); **1** done (d0bdd1c);
**2** done (6b338ae, 6ec2a46, 16e6304); **3** done in code (c276ecd, 77ee47e, 91de773, 062dcbf), manual device tests
open; **4** done in code (4513802, 0e195f3, 727ad14, faf7ab9), Play Console and upgrade test open; **5** not started.
See the checkboxes in [05](05-plan-and-verification.md).

## Biggest risks (details in 05)

1. **Data loss on update** — Java-serialized `Ussd` without `serialVersionUID`. (UID now pinned; R8 keep rule added; the upgrade test over the real old APK is still open.)
2. **Accessibility-based USSD capture** on current Android/OEMs and **Google Play accessibility policy**.
3. **minSdk 16 → 23** drops a (now tiny) device slice; alternative is pinning older AndroidX (see 02).
4. **ButterKnife** is incompatible with AGP 8+/9 default non-final R fields — must be removed or reconfigured. (Removed.)
5. **Material FAB crashes at runtime** if the app theme is not a Material Components theme. (`Theme.Ussd` set on the application.)
