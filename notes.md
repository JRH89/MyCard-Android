# Project Notes - Release Preparation

## Changes Made (May need review/reversion)

### 1. Build & Versioning
- **Version Reset**: Reset `versionCode` to `1` and `versionName` to `"1.0"` for a fresh Google Play Store account (previous account was deleted/inactive).
- **Minification**: Enabled `minifyEnabled true` in `app/build.gradle` for release builds. Ensure to test the release build thoroughly as ProGuard/R8 can sometimes break reflection or certain libraries.

### 2. Firebase Configuration
- **google-services.json**: The `google-services.json` file was missing, causing build failures. It has been added to the `app/` directory. Ensure this matches the correct Firebase project for the new production account.

### 3. Manifest Clean-up
- **SupportActivity**: Removed the `.SupportActivity` entry from `AndroidManifest.xml` because the source file was missing/ignored. If this activity is needed later, the file must be restored and the manifest entry re-added.

### 4. Permissions
- Current permissions in manifest: `INTERNET`, `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`. Ensure these are all still required for the current version of the app to comply with Play Store policies.
