# 05 — Ordered plan, verification, risks

Tick boxes as work lands. Each phase should be its own commit (or PR) so any step can be reverted independently.
Do **not** run `clean` as a verification step.

## Phase 0 — Safety net (before touching anything)
- [ ] Tag the current commit (`git tag pre-modernization`).
- [ ] Locate/back up `~/.android/dmax.keystore` and its password (needed to sign updates unless Play App Signing is used).
- [ ] Build the **last released** APK (or take it from the Play Console / device) and keep it for the upgrade test.
- [ ] Compute the current `Ussd` serialVersionUID from that build and pin it:
  `serialver -classpath <dir-with-compiled-classes> com.tooploox.ussd.domain.Ussd` →
  `private static final long serialVersionUID = …L;`
- [ ] Decide minSdk (**recommended 23**, see [02](02-dependencies.md#2-minsdk-consequence-important)) and targetSdk (36).
- [ ] Decide: ViewBinding (recommended) vs ButterKnife stopgap; keep `LocalBroadcastManager` or replace.

## Phase 1 — Toolchain ([01](01-build-tooling.md), [03](03-build-script.md))
- [ ] Install/verify JDK 17 or 21, SDK `platforms;android-36`, `build-tools;36.0.0`, accept licenses.
- [ ] Add `settings.gradle` (repos, `rootProject.name`).
- [ ] Edit wrapper to Gradle 9.6.1+ (`-bin`), then run `./gradlew wrapper --gradle-version … --gradle-distribution-sha256-sum …`.
- [ ] Rewrite `build.gradle` to AGP 9.5.1 with the new DSL (namespace, `compileSdk`, no `buildToolsVersion`, `buildFeatures`, `lint {}`).
- [ ] Remove `keystorePassword=` from `gradle.properties`; add performance flags.
- [ ] `./gradlew help` ✔ `./gradlew build --dry-run` ✔ (source will not compile yet — expected).

## Phase 2 — AndroidX ([02](02-dependencies.md))
- [ ] Replace dependencies with AndroidX/Material/ConstraintLayout.
- [ ] Apply Java import mapping (6 files) and layout XML mapping (4 layouts).
- [ ] Replace ButterKnife with ViewBinding in `SetupActivity` and `UssdListActivity` (flatten helper inner classes).
- [ ] Switch adapter to `androidx.databinding.*` (optionally use `UssdListItemBinding` directly).
- [ ] Create Material 3 theme `Theme.Ussd`; replace `?attr/colorAccent`.
- [ ] `./gradlew assembleDebug` ✔

## Phase 3 — Platform behaviour ([04](04-platform-behavior.md))
- [ ] Manifest: remove `package`, add `exported` ×3, application-level theme, backup policy decision.
- [ ] Edge-to-edge handling on both activities; verify FAB and list not obscured on gesture and 3-button nav.
- [ ] `UssdAccessibilityService`: null checks, act only when a run is pending, review `service_config.xml` flags.
- [ ] `UssdSimpleStorage.getUssdList`: skip `null` entries; stable ordering (sort by insertion/time/code) if desired.
- [ ] Remove `@SuppressLint("NewApi")` (minSdk ≥ 23 makes it unnecessary).
- [ ] Delete stray `src/main/res/res/`.
- [ ] Manual test on API 36 emulator + one physical device with a SIM (USSD cannot be fully tested on an emulator).

## Phase 4 — Hardening / release
- [ ] R8: `proguard-android-optimize.txt` + `proguard-rules.pro` (keep `Ussd`); test release build + upgrade-over-old-APK.
- [ ] `./gradlew lint` → triage, create `lint-baseline.xml`, set `abortOnError = true`.
- [ ] Signing via env/properties (see 03); `./gradlew assembleRelease` locally with the keystore.
- [ ] GitHub Actions workflow; delete `.travis.yml`.
- [ ] Rewrite `POLICY.md` to match actual behaviour; fix strings typo ("quite codes" → "quick codes").
- [ ] Play Console: Accessibility API declaration, Data safety form, target-API requirement, bump `versionCode` > 2.

## Phase 5 — Optional modernization (separate PRs)
- [ ] Move to `app/` module layout; Kotlin DSL + `libs.versions.toml`.
- [ ] Add `TelephonyManager.sendUssdRequest` executor (API 26+) and make accessibility the fallback.
- [ ] Replace `LocalBroadcastManager`/`EventBus` with `LiveData`/Flow; introduce Android `ViewModel`.
- [ ] Replace Java-serialization storage with Room or DataStore (one-time migration reading the old prefs; keep
      the pinned UID class around until migration code is removed).
- [ ] Unit tests (presenter, repository) + a minimal instrumented smoke test; consider the `testing-setup` skill.
- [ ] Optional port to Kotlin/Compose (AGP 9 built-in Kotlin; see `agp-9-upgrade` skill for Kotlin-related steps).

## Verification checklist (end state)
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

## Rollback
Everything above is source/config only. `git revert` per phase; Phase 0 tag `pre-modernization` marks the baseline.
No data migration happens before Phase 5, so rolling back an install is safe only if you did not change the stored
format — keep that in mind before shipping Phase 5 storage replacement.
