# ScamShield AI

> **BEFORE YOU CLICK, PAY OR REPLY — CHECK IT WITH SCAMSHIELD.**

## Automated GitHub APK Builds

This repository is configured with a GitHub Actions workflow (`.github/workflows/build-apk.yml`) that automatically builds the Android APK whenever you:

- Push commits to `main` or `master`
- Open a pull request against `main` or `master`
- Push a version tag like `v1.0.0`
- Trigger a manual build via **Actions → Build Android APK → Run workflow**

### Downloading the Built APK

1. Open your repository on GitHub and click the **Actions** tab.
2. Select the latest **Build Android APK** workflow run.
3. Under **Artifacts**, download **ScamShield-AI-debug-apk** (contains `app-debug.apk`, signed with the project's registered debug keystore so Google Sign-In and Firebase work out of the box).
4. If you push a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`), the APK is also attached directly to the GitHub **Releases** page.

### Optional: Signed Release APK Builds on GitHub

To also produce a signed `app-release.apk` in GitHub Actions, add the following **Repository Secrets** in **Settings → Secrets and variables → Actions**:

- `RELEASE_KEYSTORE_BASE64`: Base64-encoded `.jks` upload keystore (with key alias `upload`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password
