#!/usr/bin/env bash
# Sign the PATCHED ORIGINAL application APK (not the standalone prototype).
# Requires environment variables APK_SIGNING_KEYSTORE, APK_SIGNING_PASSWORD,
# APK_SIGNING_ALIAS and APKSIGNER (Android SDK apksigner binary).
# Example: bash sign_original_apk.sh workout-patched-classes5.apk fixed.apk
#
# Preserve this signing key: future patched releases must use the same key.
set -euo pipefail
INPUT="$1"
OUTPUT="$2"
: "$APK_SIGNING_KEYSTORE"
: "$APK_SIGNING_PASSWORD"
: "$APK_SIGNING_ALIAS"
: "$APKSIGNER"

# Force the final ZIP layout to meet 16-KiB page alignment on Android 16.
# JAR v1 signing is unnecessary (the original minSdk is 25).
# DO NOT rewrite the ZIP after signing; that invalidates v2/v3 signatures.
"$APKSIGNER" sign \
  --ks "$APK_SIGNING_KEYSTORE" \
  --ks-pass "env:APK_SIGNING_PASSWORD" \
  --ks-key-alias "$APK_SIGNING_ALIAS" \
  --v1-signing-enabled false \
  --v4-signing-enabled false \
  --lib-page-alignment 16384 \
  --out "$OUTPUT" "$INPUT"
"$APKSIGNER" verify --verbose "$OUTPUT"
python3 "$(dirname "$0")/verify_installable.py" "$OUTPUT"
echo "Validated APK: $OUTPUT"
