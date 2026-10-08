# Automatic Releases

## Automatic patch releases

The GitHub Actions workflow runs on every push to `main`, including a pull request merge. It does not run when a branch is created, and it does not inspect branch names, commit messages, or pull request labels. Each push to `main` starts a release build.

The workflow reads the latest published GitHub Release and expects its tag to use `vMAJOR.MINOR.PATCH`, for example `v1.0.2`. It increments the patch number, updates the version in the previous release title when possible, builds the application, and publishes the new release as the latest release. For example, the next automatic release after `v1.0.2` is `v1.0.3`.

The release contains these versioned files:

- `VpsManager-<version>-Setup.exe`
- `VpsManager-<version>.jar`
- `VpsManager-<version>-app-image.zip`

The portable app image is built for Windows by this workflow. A failed build does not publish a release. The workflow requires an existing published release with a valid version tag to determine the next version.

## Major or minor releases

Major and minor version bumps are not selected automatically. To make one, build the artifacts and publish a GitHub Release manually with the intended semantic version tag and title. For example:

- Breaking changes: `v2.0.0`
- New backward-compatible features: `v1.1.0`

Use the `vMAJOR.MINOR.PATCH` tag format and include the new version in the release title, such as `VPS Manager 2.0.0 (Stable Release)`. Upload the JAR, installer, and portable app image archive. Once that release is published, later pushes to `main` will continue with patch releases from its version, such as `v2.0.1`.

To build a chosen version locally, set `VERSION` before running `build.bat` in the same Command Prompt window:

```bat
set "VERSION=2.0.0"
build.bat
```

`build.bat` uses `1.0.2` as its local default when `VERSION` is not set. The GitHub Actions workflow supplies its calculated version through that environment variable, so the local default does not control automated release versions.

## In-app updates

On Windows, the application checks GitHub's latest published release endpoint at startup. GitHub's latest-release endpoint excludes drafts and prereleases, so preview builds are not offered. If the stable release tag is newer than the running version, the app downloads `VpsManager-<version>-Setup.exe` into the user's application data directory and asks whether to run the installer. The installer only starts after the user confirms. Choosing **Later** (or closing the prompt) records that version as deferred, suppresses further automatic prompts for that version, and leaves an update badge on **About**. The user can install the downloaded update later from **About**. The app prompts automatically again when a newer version is available. The **About** button displays the running version and provides a manual check. Update checks and downloads run in the background so the main window remains responsive.

Automatic installer updates are Windows-only because the release workflow builds a Windows installer. The self-contained JAR remains available for other platforms through GitHub Releases.