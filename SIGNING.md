# Persistent Android release signing (v1.6.1 onward)

Duodecimal Workout uses a long-lived **release signing identity**, kept in
GitHub **encrypted Actions repository secrets**, not committed publicly.

## One-time setup

In repository `kd40000-dot/math-workout-duodecimal`, open
**Settings → Secrets and variables → Actions → New repository secret**.

Create these two repository secrets:

1. `DUO_SIGNING_KEYSTORE_B64`: paste the **entire single line** from
   `DUO_SIGNING_KEYSTORE_B64.txt` supplied with the signing setup.
2. `DUO_SIGNING_PASSWORD`: paste the contents of
   `DUO_SIGNING_PASSWORD.txt` (omit trailing newline).

The key alias is `duodecimal`. The keystore is PKCS#12 format.
Keep an offline backup of the original `.keystore` file and password.
The same exact certificate must be used for every future build.
**Do not upload any signing files to GitHub source commits, Issues, or Releases.**

### Expected certificate fingerprint

SHA-256:
`74:36:E5:7A:25:BC:8E:75:3C:C4:75:6F:58:78:8B:11:98:BC:BA:B3:01:18:8C:0C:44:EF:99:9F:0B:0F:81:4B`

The workflow checks this signature before publishing. Missing or wrong
secrets fail the build instead of silently reverting to a temporary debug key.

## Future upgrades

All versions installed from the **v1.6.1 signed release onward** can update
each other in place, as long as the package ID stays
`com.duodecimal.workout`, the signature stays identical, and versionCode
increases. The app's local learning progress survives an in-place update.

**Important**: existing APKs previously built with temporary debug keys
cannot be updated in place using this new signing key. Android prevents
signature changes for the same package. This signing migration may require one
final backup/reinstall. Back up the current app's private data first, especially
progress; uninstalling deletes it. Do not uninstall without confirming a viable
backup/restore method if you need to retain that data.

## Build

The workflow tests and runs `gradle :app:assembleRelease`.
It restores the keystore in the GitHub runner's temporary directory from
encrypted repository secrets and publishes `app-release.apk` in the workflow
artifact and GitHub Release.

To retry once secrets are configured: **Actions → Build Duodecimal Workout APK → Run workflow**.
