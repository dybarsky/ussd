# 05 — Ordered plan, verification, risks

Status 2026-11-17: boxes ticked `[x]` are done **and** checked against the repo (or a build) on branch
`modernize/agp-9`; `[ ]` boxes are not done or not verified (see notes). The [execution log](#execution-log) at the
end lists the commits and deviations from this plan.

Tick boxes as work lands. Each phase should be its own commit (or PR) so any step can be reverted independently.
Do **not** run `clean` as a verification step.

## Phase 0 — Safety net (before touching anything)
- [ ] Tag the current commit (`git tag pre-modernization`). Not done; the existing tag `0.2` ("Version up") marks the released baseline.
- [ ] Locate/back up `~/.android/dmax.keystore` and its password (needed to sign updates unless Play App Signing is used). The keystore exists locally and signed a local release build; an off-machine backup was not verified.
- [ ] Build the **last released** APK (or take it from the Play Console / device) and keep it for the upgrade test. Not done.
- [x] Compute the current `Ussd` serialVersionUID from that build and pin it:
  `serialver -classpath <dir-with-compiled-classes> com.tooploox.ussd.domain.Ussd` →
  `private static final long serialVersionUID = …L;`
  Done in 574984d: `-2632603246578927450L`, computed from the original class with `javac --release 8` and with JDK 25
  `javac` (identical). Computed from source of tag `0.2`, not from the actual released APK.
- [x] Decide minSdk (**recommended 23**, see [02](02-dependencies.md#2-minsdk-consequence-important)) and targetSdk (36). Chosen: 23 / 36.
- [x] Decide: ViewBinding (recommended) vs ButterKnife stopgap; keep `LocalBroadcastManager` or replace. Chosen: ViewBinding; `LocalBroadcastManager` kept.

## Phase 1 — Toolchain ([01](01-build-tooling.md), [03](03-build-script.md))
- [ ] Install/verify JDK 17 or 21, SDK `platforms;android-36`, `build-tools;36.0.0`, accept licenses. SDK platform 36 and build-tools 36.0.0 are present; the build was run on JDK 25 (Android Studio JBR) because no JDK 17/21 is installed — JDK 17/21 remain unverified.
- [x] Add `settings.gradle` (repos, `rootProject.name`).
- [x] Edit wrapper to Gradle 9.6.1+ (`-bin`), then run `./gradlew wrapper --gradle-version … --gradle-distribution-sha256-sum …`. Result in repo: `gradle-9.6.1-bin.zip`, `distributionSha256Sum` set, wrapper jar/scripts regenerated (d0bdd1c).
- [x] Rewrite `build.gradle` to AGP 9.5.1 with the new DSL (namespace, `compileSdk`, no `buildToolsVersion`, `buildFeatures`, `lint {}`).
- [x] Remove `keystorePassword=` from `gradle.properties`; add performance flags.
- [x] `./gradlew help` ✔ `./gradlew build --dry-run` ✔ (source will not compile yet — expected). Re-run 2026-11-17: `help` and `build --dry-run` succeed; `assembleDebug` and `lintDebug` are green at HEAD.

## Phase 2 — AndroidX ([02](02-dependencies.md))
- [x] Replace dependencies with AndroidX/Material/ConstraintLayout.
- [x] Apply Java import mapping (6 files) and layout XML mapping (4 layouts).
- [x] Replace ButterKnife with ViewBinding in `SetupActivity` and `UssdListActivity` (flatten helper inner classes). `UiEventsReactor` kept as an inner class; the other holders were removed.
- [x] Switch adapter to `androidx.databinding.*` (optionally use `UssdListItemBinding` directly). The `androidx.databinding.*` switch is done; the optional `UssdListItemBinding` change was not made.
- [x] Create Material 3 theme `Theme.Ussd`; replace `?attr/colorAccent`. (`colorSecondary` used; no brand colours defined.)
- [x] `./gradlew assembleDebug` ✔

## Phase 3 — Platform behaviour ([04](04-platform-behavior.md))
- [x] Manifest: remove `package`, add `exported` ×3, application-level theme, backup policy decision. (`exported`: `UssdListActivity` true, `SetupActivity` false, service true; `allowBackup=false`; `supportsRtl=true`.)
- [x] Edge-to-edge handling on both activities (code, 77ee47e).
- [ ] Verify FAB and list not obscured on gesture and 3-button nav (visual check not recorded).
- [x] `UssdAccessibilityService`: null checks, act only when a run is pending (`UssdExecutor.hasPending()`), review `service_config.xml` flags (`flagReportViewIds`, `notificationTimeout=100` added). Code only; behaviour on real devices unverified.
- [x] `UssdSimpleStorage.getUssdList`: skip `null` entries; stable ordering (sort by insertion/time/code) if desired. Skips unreadable entries; sorted by code (case-insensitive, nulls last), then id.
- [x] Remove `@SuppressLint("NewApi")` (minSdk ≥ 23 makes it unnecessary). (`MissingPermission` suppress in `UssdRunner` remains.)
- [x] Delete stray `src/main/res/res/` (unreferenced alternate icon renderings, not duplicates).
- [ ] Manual test on API 36 emulator + one physical device with a SIM (USSD cannot be fully tested on an emulator). Only ad-hoc emulator use (API 37) is known, which found the `*`/`#` keyboard problem; no SIM-device test.

## Phase 4 — Hardening / release
- [x] R8: `proguard-android-optimize.txt` + `proguard-rules.pro` (keep `Ussd`); release build compiles (`assembleRelease`; `mapping.txt` is produced).
- [ ] Test release build on a device + upgrade-over-old-APK (open).
- [x] `./gradlew lint` → triage, create `lint-baseline.xml`, set `abortOnError = true`. (`lintDebug` passes; 10 baselined findings, listed in [context.md](../context.md).)
- [x] Signing via env/properties (see 03); `./gradlew assembleRelease` locally with the keystore. Exercised only locally (the release APK on disk is signed with the real key via `~/.gradle/gradle.properties` + `~/.android/dmax.keystore`); no CI signing.
- [x] GitHub Actions workflow; delete `.travis.yml`. (727ad14)
- [ ] First green run of the workflow on GitHub (never run; SDK availability on the runner unverified).
- [x] Rewrite `POLICY.md` to match actual behaviour; fix strings typo ("quite codes" → "quick codes"). (faf7ab9)
- [x] Play rejection "Missing prominent disclosure" (2026-11-18): in-app 3-step wizard with accessibility disclosure and consent, new icon, listing text. `versionCode` 4 / `0.4`. See [context.md](../context.md#google-play-accessibility-policy-why-the-wizard-exists).
- [ ] Play Console: Accessibility declaration + demo video, Data safety form, new listing text/icon/feature graphic/screenshots ([store/listing.md](../store/listing.md)), then resubmit 0.4 (open; needs the maintainer).

## Phase 5 — Optional modernization (separate PRs)
- [ ] Move to `app/` module layout; Kotlin DSL + `libs.versions.toml`.
- [ ] Add `TelephonyManager.sendUssdRequest` executor (API 26+) and make accessibility the fallback.
- [ ] Replace `LocalBroadcastManager`/`EventBus` with `LiveData`/Flow; introduce Android `ViewModel`.
- [ ] Replace Java-serialization storage with Room or DataStore (one-time migration reading the old prefs; keep
      the pinned UID class around until migration code is removed).
- [ ] Unit tests (presenter, repository) + a minimal instrumented smoke test; consider the `testing-setup` skill.
- [ ] Optional port to Kotlin/Compose (AGP 9 built-in Kotlin; see `agp-9-upgrade` skill for Kotlin-related steps).

## Verification checklist (end state)
Status per item on 2026-11-17: **1** verified (Gradle 9.6.1, launcher JVM 25.0.4). **2** `assembleDebug` and
`lintDebug` re-run green; `assembleRelease` green per commit messages 4513802/0e195f3 and a release APK + `mapping.txt`
exist in `build/`; `help` and `build --dry-run` re-run green. **3** not verified locally (CI step uses `--warning-mode all` but has
never run). **4** partial: an API 37 emulator was used ad hoc, the full flow on API 36 is not recorded. **5** open.
**6** open. **7** partial: R8 enabled and mapping file produced; no logcat check on a device. **8** open.

1. `./gradlew --version` → Gradle ≥ 9.6, JVM ≥ 17.
2. `./gradlew help`, `./gradlew build --dry-run`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew lint`.
3. No deprecation warnings from AGP DSL (`./gradlew assembleDebug --warning-mode all`).
4. Installs and launches on an API 36 emulator; first launch → Setup screen → permission + accessibility rows turn green → list.
5. On a SIM device: add `*#06#`-style or operator balance code → tap → dialog dismissed → response shown in list.
6. Upgrade test: old release APK with saved codes → install over it → codes + responses intact.
7. Release APK: R8 enabled, no `ClassNotFoundException`/`InvalidClassException` in logcat, mapping file produced.
8. Layout check at large font, RTL (pseudo-locale), dark mode, tablet/foldable window sizes, gesture + 3-button nav.

## Risk register

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Stored `Ussd` objects unreadable after update (no `serialVersionUID`, obfuscation, class changes) | Medium | High (user data loss) | Phase 0 pin UID; keep FQN/fields; `-keep` rule; null-tolerant list; upgrade test |
| R2 | USSD dialog not detected on newer Android/OEMs (ids/package differ) | High (pre-existing) | High (core feature) | Telephony API executor + accessibility fallback; device matrix test |
| R3 | Google Play rejects accessibility usage | Medium | High | Declaration + in-app disclosure, or drop accessibility via `sendUssdRequest` |
| R4 | minSdk 16 → 23 removes some users | Certain | Low | Accept; announce in release notes. Alternative (old AndroidX pins) rejected |
| R5 | ButterKnife/non-final R compile errors | Certain if kept | Medium | Replace with ViewBinding (Phase 2) |
| R6 | Material FAB crash due to non-Material theme | Certain if forgotten | Medium (crash on launch) | Theme.Ussd Material 3 (Phase 2) |
| R7 | Edge-to-edge hides FAB/list | Certain on targetSdk 35+ | Medium | Phase 3 insets handling |
| R8 | R8 now optimizes/obfuscates → release-only crashes | Medium | Medium | Test release build; start with `-dontobfuscate` if needed |
| R9 | Gradle/AGP incompatibility with newer Gradle (9.8/9.9) deprecations | Low | Low | Use 9.6.1 first; bump after green; AGP 9.5.0+ includes the Gradle 9.8 `setVisible` fix |
| R10 | Release signing unavailable on CI / lost keystore | Medium | High (cannot update the app) | Verify keystore backup in Phase 0; Play App Signing |
| R11 | `POLICY.md` inconsistent with real behaviour | Certain today | Medium (policy/review) | Phase 4 rewrite |
| R12 | JDK 25 default on this machine vs Studio JBR | Low | Low | Pin `JAVA_HOME`/Studio Gradle JDK to 17 or 21 |

Risk status on 2026-11-17: R1 mitigated in code (UID pinned, keep rule, null-tolerant list) but the upgrade test is
open; R2 unchanged (pre-existing, untested on OEM devices); R3 open; R4 accepted (minSdk 23); R5, R6, R7 addressed in
code (visual check of R7 open); R8 open until a release build is run on a device; R9 not hit (Gradle 9.6.1 used);
R10 partly (keystore exists locally, no backup verification, no CI signing); R11 fixed (faf7ab9); R12 occurred as
described (JDK 25 only; it worked).

## Execution log
Commits on `modernize/agp-9` after the docs commit 93660d1, in order:

| Commit | Subject | Plan phase |
|---|---|---|
| 574984d | Pin Ussd serialVersionUID to keep stored data readable | 0 |
| ac267c5 | Remove accidental nested res/res directory | 3 |
| d0bdd1c | Migrate build to Gradle 9.6.1 and AGP 9.5.1 | 1 |
| 6b338ae | Migrate Java imports from Support Library to AndroidX | 2 |
| 6ec2a46 | Migrate layouts and theme to AndroidX and Material 3 | 2 |
| c276ecd | Declare android:exported and backup policy in manifest | 3 |
| 16e6304 | Replace ButterKnife with ViewBinding | 2 |
| 77ee47e | Handle edge-to-edge insets on targetSdk 36 | 3 |
| 91de773 | Harden accessibility service against null nodes and stray dialogs | 3 |
| 062dcbf | Skip unreadable entries when loading stored USSD codes | 3 |
| 4513802 | Switch to R8 optimize defaults with keep rule for Ussd | 4 |
| 0e195f3 | Enable lint enforcement with baseline | 4 |
| 0488f7a | Add * and # buttons to the add-USSD dialog (later reverted, see below) | not in plan |
| 727ad14 | Replace Travis with GitHub Actions | 4 |
| faf7ab9 | Align privacy policy with actual app behavior | 4 |

Deviations and observations:
- The manifest `exported` fix (c276ecd) was done **before** the ButterKnife replacement (16e6304) because the manifest
  merger fails before Java compilation otherwise.
- Compilation was red between the toolchain commit (d0bdd1c) and the ButterKnife commit (16e6304), as the plan
  expected (Phase 1 "source will not compile yet").
- The build ran on JDK 25 (Android Studio JBR) because no JDK 17/21 is installed; it worked. The plan's "17 minimum /
  21 recommended" guidance is unchanged; 25 is now also verified.
- An untracked `gradle/gradle-daemon-jvm.properties` appeared (generated by Gradle's `updateDaemonJvm`; pins the
  daemon JVM to 25). It was deliberately **not** committed and is not git-ignored.
- `0488f7a` was not planned: on an API 37 emulator, Gboard's phone dialpad did not type `*`/`#` in the add dialog,
  so the dialog got insert buttons. On the maintainer's physical phone Gboard types them fine, so the buttons were
  **reverted** ("Remove * and # buttons from the add-USSD dialog"); the emulator behaviour looks emulator-specific
  and the field is back to a plain `inputType=phone`.
- Lint (0e195f3): beyond baselining 10 findings, cheap findings were fixed (dialog top constraint and `labelFor`,
  `importantForAutofill`, FAB content description/focusable, `importantForAccessibility="no"` on setup icons,
  removal of unused `colors.xml` and one unused dimen).
- R8 inlines `UssdRunner` into its callers in the release build; considered normal.
- New open quirks found along the way (no UI to delete a code, dialog does not auto-focus, Material 3 accent change)
  are recorded in [context.md](../context.md).
- Still open: see the unticked boxes above (upgrade-over-old-APK test, SIM/OEM test, first GitHub Actions run, Play
  Console, visual checks on dark mode / large font / RTL / tablets, Phase 5).

## Rollback
Everything above is source/config only. `git revert` per phase; Phase 0 tag `pre-modernization` marks the baseline.
No data migration happens before Phase 5, so rolling back an install is safe only if you did not change the stored
format — keep that in mind before shipping Phase 5 storage replacement.
