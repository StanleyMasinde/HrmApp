# Contributing

Thanks for contributing to HrmApp.

## Before you open a PR

- Test on a real Wear OS watch when the change touches BLE, permissions, sensors, or foreground-service behavior.
- Keep changes focused. Small PRs are much easier to review than broad refactors.
- Update [README.md](README.md) when user-facing behavior or setup steps change.

## Setup

1. Install Android Studio.
2. Open the project.
3. Use a Wear OS device with a heart-rate sensor for validation when possible.

Useful commands:

```bash
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

## Reporting bugs

Include:

- watch model
- Wear OS / Android version
- receiver or fitness app used for pairing
- whether heart-rate readings appear in the watch UI
- steps to reproduce
- logs or screenshots if available

## Pull request guidelines

- Describe the problem first, then the fix.
- Mention how you tested the change.
- Call out any device-specific assumptions.
- Avoid unrelated formatting or file churn in the same PR.

## Scope

Areas that need extra care:

- background permission flow
- BLE advertising and reconnect behavior
- heart-rate availability handling
- foreground-service lifecycle

If you are not sure whether a behavior change is safe, open an issue first.
