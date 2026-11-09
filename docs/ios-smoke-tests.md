# iPhone and iPad startup / HTTPS smoke tests

Run the Codemagic workflow **iPhone and iPad Release Smoke Tests** (`ios-build-test`) on the desired commit. A push to `main` also triggers this workflow. It does not publish an IPA or submit an app for review.

The workflow builds an unsigned **Release simulator app** and uses the newest installed iOS runtime with both an available iPhone and iPad. It prefers iPhone 17 Pro Max and iPad Air 11-inch (M3) if available on that runtime; otherwise the actual selected models and runtime are recorded in the results. This is not a claim of testing on Apple's exact review hardware or OS.

For each device family the runner creates a fresh simulator, boots it, installs the app, then launches it with an attached console for at least 60 seconds. Existing simulator devices are not shut down or deleted. The temporary devices created by the test are cleaned up afterwards.

The test passes only when:

- The UI reports appearing and the attached app stays alive for the full observation period (`KITAPDUY_CI_UI_APPEARED`).
- The app's **Ktor Darwin** client completes a real HTTPS GET, reads response headers and a non-empty response body, and reports a 2xx response (`KITAPDUY_CI_NETWORK_PASS`). This exercises the `readHeaders` path from Build 24.
- Automated Guest Login succeeds and the authenticated Home screen books/categories load (`KITAPDUY_CI_GUEST_LOGIN_PASS`, `KITAPDUY_CI_HOME_LOADED`).
- Real audiobook playback through `AVPlayer` / `IosAudioPlayer` starts and progresses actively (`KITAPDUY_CI_AUDIO_PLAYING`).
- Core tabs and features navigate successfully:
  - Search tab loads (`KITAPDUY_CI_TAB_SEARCH_PASS`).
  - Feed tab loads and post creation flow succeeds (`KITAPDUY_CI_TAB_FEED_PASS`, `KITAPDUY_CI_FEED_POST_PASS`).
  - Profile tab loads and avatar update capability succeeds (`KITAPDUY_CI_TAB_PROFILE_PASS`, `KITAPDUY_CI_PROFILE_UPDATE_PASS`).
  - Complete simulation finishes with `KITAPDUY_CI_USER_SIMULATION_PASS`.
- No networking failure, simulation error, or unhandled Kotlin error marker is logged.
- Screenshots are captured at each key milestone (`01_home.png`, `02_player.png`, `03_search.png`, `04_feed.png`, `05_profile.png`, `screen.png`).

The default GET endpoint is `https://example.com/`; it receives no app credentials or user data. Set the Codemagic variable `IOS_SMOKE_HTTPS_URL` to another public HTTPS endpoint returning a 2xx status and a non-empty body if needed. URLs with embedded credentials are rejected.

All automated test simulation drivers are strictly isolated to simulator/CI environments (`#if targetEnvironment(simulator)` in Swift and `KITAPDUY_CI_SMOKE_TEST=1` check in Kotlin). Normal app launches and physical-device App Store release builds never invoke them.

## Evidence in Codemagic Artifacts

- `build/ios-smoke/summary.json`: selected devices, runtime, pass/fail, captured screenshots and errors.
- `build/ios-smoke/junit.xml`: test results shown by Codemagic.
- `build/ios-smoke/iPhone/` and `iPad/`:
  - `01_home.png`: Authenticated home screen with books/categories.
  - `02_player.png`: Active audiobook player/detail screen.
  - `03_search.png`: Search tab view.
  - `04_feed.png`: Social feed screen.
  - `05_profile.png`: Profile tab view.
  - `screen.png`: Final state screenshot.
  - `console.log`, `system.log`, `result.json`, and matching crash reports if any.
- `build/ios-smoke/runner.log` and `devices.json`: simulator commands and installed device inventory.
- Existing compiler/Xcode logs remain available as artifacts.

A crash, incomplete HTTPS probe, missing runtime/device family, timeout or screenshot failure fails the workflow. Both device families are attempted even if the first launch fails.

These are automated startup and transport smoke tests, not a complete login/playback/UI suite or physical-device certification. Inspect the screenshots before submission. TestFlight on real iPhone/iPad hardware remains the next level of validation.

## Local checks

The runner's failure handling and device selection can be checked without macOS:

```text
python -m unittest discover -s ci -p 'test_run_ios_smoke.py'
```

Running the actual simulator test requires macOS/Xcode and the simulator `.app` built by the workflow:

```sh
python3 ci/run-ios-smoke.py \
  --app build/ios-simulator/Build/Products/Release-iphonesimulator/KitapDuy.app \
  --output build/ios-smoke --duration 60
```
