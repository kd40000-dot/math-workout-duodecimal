# Duodecimal Workout — standalone Android app

This repository contains **two different projects**. For the current user's request, the active app is the **separate, original duodecimal workout app** with package `com.duodecimal.workout`, found under `app/`. The `reverse-patch/` folder contains an unrelated, experimental patch for Math Tricks Workout 2.8.5 and is retained for reference only.

## Version 1.4.0 — Visual Addition Training

Tap **VISUAL ADDITION · CAP + BODY →** on the standalone Duodecimal Workout home
screen. This is not a patch to the proprietary Math Tricks Workout game.

The dedicated training screen teaches how to add numbers using our Draft A
modular numeral system, without changing the existing custom practice or
standard workout modes. Five practice modes are available:

1. **Caps · mod 3:** select the resulting flat bar, Vietnamese breve, or dot.
   Each pass contains all nine ordered cap-pair combinations.
2. **Bodies · mod 4:** select the resulting loop, short ⊺, V, or regular-weight
   ↋ body. Each pass contains all sixteen ordered body-pair combinations.
3. **Combine · no carry:** select cap and then body; all presented sums are
   below `10₁₂` (12 decimal).
4. **Carry recognition:** choose whether each one-digit addition produces a
   carry. Both carry and no-carry cases are included.
5. **Full visual addition:** answer the cap, body, and carry questions in
   sequence to recover the complete base-twelve sum.

All five use the actual **vector component paths** in
`app/src/main/java/com/duodecimal/workout/Numerals.java`, never approximate
unicode characters for the isolated components. The complete addends,
result, practice counts, and timer are presented in base twelve using the
existing font-independent renderer. Separate component and drill accuracy
statistics are stored locally. Wrong answers can be retried without recording
multiple attempts, while first-try accuracy is tracked. Correct selections
advance automatically with a short confirmation and the finished sum is shown.

Options include 10₁₂, 20₁₂, or 40₁₂ questions, guided hints, an optional
traditional-notation aid, a one-minute timed challenge, and tappable reference
tables for the cap/body addition patterns.

The implementation is split into the pure Java `VisualAddition.java` arithmetic
engine, `VisualAdditionActivity.java` for the UI, and a
`VisualAdditionTest.java` unit test that exhaustively checks all **144**
single-digit ordered sums and their cap/body/carry values.

## Version 1.3.0 — Draft A modular numerals

The standalone app now **renders all base-twelve digits** in the custom glyph
system: body = digit modulo 4; cap = digit modulo 3. This applies to regular
workout prompts, entered answers, keypad labels, feedback, scores, session
counts, Custom Practice table results, row/column headers, and spinner items.
The source of truth is `app/src/main/java/com/duodecimal/workout/Numerals.java`.

Bodies: rounded rectangular zero (0 mod 4), short-bar `⊺` (1 mod 4),
V (2 mod 4), and the **exact regular-weight outline of U+218B ↋** (3 mod 4).
Caps: flat horizontal, Vietnamese breve (as in `ă`), and dot, corresponding
to residues modulo 3 of 0, 1, and 2.

The app draws these shapes directly as Android vector paths in replacement
spans; **no font files are bundled or required**. Number strings still use
standard underlying 0–9/↊/↋ code points for arithmetic and accessibility.
Base-12 calculations, saved fact profiles and automatic acceptance of correct
answers are preserved.

Note: the GitHub Actions build uses a per-run debug signing certificate and
may not directly update a prior version. Back up selections or other data
before uninstalling an earlier version. Permanent production signing remains
future work.

## Version 1.1.0 — Custom Practice

On the home screen, tap **CUSTOM PRACTICE · SELECT FACTS →**. This opens a dedicated page where you can:

- Choose addition, subtraction, multiplication, or exact division.
- Set first-operand and second-operand lengths independently (1–4 base-12 digits).
- Tap the result cells of a selectable arithmetic table. Rows are the **first** operand; columns are the **second** operand.
- Navigate tables larger than 12×12 using row/column page controls and slider jumps. One-digit tables cover 0–↋ in both directions.
- Save independent sets of exact operand pairs for every operation and digit-length combination. Tapping `3+5` does not automatically include `5+3`.
- Select exactly 1, 2, 3, 5, `10₁₂` (12 decimal), or `20₁₂` (24 decimal) repetitions for **each** selected fact.
- Complete the shuffled practice session using the app's ↊/↋ keypad, view the score, and repeat the same selections.

For exact division, invalid/zero-divisor/non-integer cells are disabled. All values, page ranges, repetitions, session counts, prompts, answers and results use base twelve.

### Building the standalone Android app

Use **Actions → Build Duodecimal Workout APK** or run:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The workflow publishes the standalone app to GitHub Releases. Android debug signing keys may differ across GitHub Actions runners, so updating an older debug-signed installation may require uninstalling the old build. A permanent signing secret is recommended before relying on seamless future upgrades.

---

## Original Math Tricks Workout — Draft A glyph patch (2026-11-12)

The latest **private test APK** incorporates Draft A numeral glyphs on top of
the previously patched Math Tricks Workout 2.8.5 original binary. The original
package is still `com.sankhyantra.mathstricks` and the original user data
format is unchanged.

The glyphs are drawn from the **same** `Numerals.java` and `ModularDigits.java`
source as standalone Duodecimal Workout 1.3.0. We compile these two classes
along with `reverse-patch/java/com/sankhyantra/mathstricks/duo/OriginalGlyphInstaller.java`
as an independent `classes6.dex` in
`.github/workflows/build-original-glyph-dex.yml`. An activity lifecycle
callback scans TextView hierarchies, adds vector glyph spans for all
base-twelve digit codepoints `0-9↊↋`, and watches dynamic text updates.
Input values and arithmetic checking are NOT altered by the renderer.

For a reproducible build, decompile the **already base-12-patched original APK**
into `reverse-patch/scripts/decoded`, then run
`reverse-patch/scripts/apply_modular_glyphs.py`, rebuild with Apktool, append
the compiled `classes6.dex`, and **sign as the last step**, with
`apksigner --lib-page-alignment 16384`. Use the same private signing key that
signed the previous NativeLib-Fixed test APK to avoid a signature mismatch.

The local binary successfully rebuilt, and has passed ZIP CRC, APK v2/v3
signature and 16-KiB alignment checks. Its native library is byte-identical
to the prior patched APK. The patch has **not** been tested on a physical
device; independent games with graphical (non-TextView) digit rendering may
require specific future patches.

**Do not upload** the proprietary original APK or its private signer keys
into this public GitHub repository.

## Archived original-app reverse-engineering notes

These files document the separate effort to modify the original Math Tricks Workout APK while preserving its package and screens.

The project previously started with a standalone prototype (`app/`, Android package `com.duodecimal.workout`). The prototype remains as a reference only, and its CI does **not** auto-publish on every patch. The old GitHub release v1.0.0 contains that **standalone prototype, not the patched original app**.

## Reproducible original-app patch

- `reverse-patch/java/com/sankhyantra/mathstricks/duo/Dozenal.java`: original-app compatibility helper for 0–9, ↊ (ten), ↋ (eleven), formatting and parsing.
- `reverse-patch/scripts/apply_patch.py`: core arithmetic question/answer rendering, input validation, and four ↊/↋ keypad layouts.
- `reverse-patch/scripts/apply_missing_number.py`: original Find Missing Number mini-game.
- `reverse-patch/scripts/apply_fill_both_blanks.py`: original Fill Both Blanks mini-game.
- `reverse-patch/scripts/apply_target_number.py`: original Target Number mini-game.
- `reverse-patch/scripts/zipalign_py.py`: post-packaging ZIP alignment.
- `.github/workflows/build-patch-dex.yml`: compiles the helper into `classes5.dex`.

The scripts expect an Apktool 3.x-decoded **original 2.8.5 APK**, with `decoded/` next to the scripts. Run in this order, once each:

```bash
python apply_patch.py
python apply_missing_number.py
python apply_fill_both_blanks.py
python apply_target_number.py
apktool b decoded -o original-unsigned.apk
```

Then append the compiled `classes5.dex` from the GitHub workflow to the unsigned APK ZIP, ZIP-align it using the included script and sign it using the Android SDK's `apksigner`. Signing must happen **after** all ZIP modifications. Never publish a source APK containing an embedded service-account key.

## What works and what remains

The binary patch builds and is ZIP-aligned and cryptographically signed with APK Signature Scheme v2/v3. The core arithmetic paths, original keypad and three independent games listed above have been modified at the smali level. The package remains the original package. **No runtime testing on a real device or emulator has yet been completed.**

This is still a **beta / incomplete conversion**: Cross Math, Lightning Round, several other independent games, lesson explanations (including hard-coded decimal examples), some progress labels, and mental-math *technique generators* that assume powers of ten still need deeper base-12 changes. It is not yet accurate to say every question is duodecimal.

The test APK must be kept out of this public repo because it contains proprietary original code/assets and potentially sensitive bundled credentials. The original app is developer-signed, and a modified APK signed with a new key **cannot install on top of it**. Back up app data before uninstalling the original, or use a separate test profile/device.

## Installation error fixed (2026-11-10)

The first original-app patch failed to install with `INSTALL_FAILED_INVALID_APK: Failed to extract native libraries, res=-2`. The original app's manifest sets `android:extractNativeLibs="false"`. Its uncompressed native `.so` must therefore be aligned to a page-size boundary inside the **finally signed APK**. The earlier build's native-library data offset was **22,161,036 bytes**, not a multiple of 4,096. Its incorrect file offset arose from the signing stage changing entry positions after alignment.

The corrected private test APK was signed with `apksigner sign --v1-signing-enabled false --v4-signing-enabled false --lib-page-alignment 16384` *directly from* the built unsigned APK containing `classes5.dex`. Its native library starts at offset **22,200,320 bytes**, exactly divisible by 16,384. Signature Scheme v2/v3 and ZIP CRC validation both pass. The native library's contents are byte-for-byte identical to the original.

See `reverse-patch/scripts/sign_original_apk.sh` and `verify_installable.py`. These scripts help detect this packaging regression, but **an actual physical-device install and startup have not yet been confirmed**.

Since this is a patch of the original APK but signed using a different key, Android's normal package manager will reject *updating* a currently installed developer-signed version with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Back up the original app data before removing it and installing the patched app. Avoid publishing the modified proprietary APK or the signing keystore in this public repo. Keep the privately generated signing key for future patch updates.

## Standalone app status

The separate `app/` folder is active again in version 1.1.0 with Custom Practice. The reverse-engineering files in `reverse-patch/` remain separate.
