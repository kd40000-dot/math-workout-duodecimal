# Math Tricks Workout 2.8.5 — original-app duodecimal patch

**Current primary goal: modify the user's ORIGINAL Math Tricks Workout APK to use base twelve**, preserving its original screens, progression, exercises and package `com.sankhyantra.mathstricks`. This is a reverse-engineering effort, **not** a replacement app.

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

## Retired standalone prototype

The `app/` folder is a separate, independent calculator trainer created earlier. It is not the target of ongoing development. The original-app reverse-engineering files in `reverse-patch/` are now authoritative.
