# Duodecimal Workout persistent signing

Since v1.6.2, releases are signed automatically using one permanent Android
certificate from `signing/duodecimal-public.p12.b64` committed in this
repository. No GitHub Actions secrets or manual setup are needed.

The password is derived deterministically by the build workflow from the
public project identifier, and is intentionally **not secret**. This is a
convenience tradeoff appropriate to this community project. Anyone with
the repository can sign APKs using the same key.

## Certificate fingerprint (SHA-256)

`74:36:E5:7A:25:BC:8E:75:3C:C4:75:6F:58:78:8B:11:98:BC:BA:B3:01:18:8C:0C:44:EF:99:9F:0B:0F:81:4B`

The workflow verifies the certificate on every build before publishing.

## Updating without reinstalling

Future APKs installed from the permanent-signing series can update in place,
provided the package remains `com.duodecimal.workout` and versionCode rises.
Android keeps the saved learning data during an ordinary upgrade.

Previously installed debug-signed versions cannot directly update to this
certificate; Android blocks a signing-identity change. If needed, preserve
progress via a backup before the one-time migration. Do not uninstall without
backing up data that matters to you.

## Build

The workflow materializes the PKCS#12 keystore in the runner's temporary
directory, sets signing environment variables, builds `assembleRelease`,
verifies the APK, uploads the artifact, and attaches it to the versioned release.
