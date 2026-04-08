# HrmApp

HrmApp is an open-source Wear OS app that turns a compatible watch into a Bluetooth Low Energy heart-rate peripheral. It reads heart-rate data from Android Health Services and exposes it over the standard Heart Rate Service (`0x180D`) so bike computers, gym equipment, and fitness apps can connect to it.

## Status

This project is usable and builds locally, but it is still early-stage software. If you rely on it during workouts, test it on your watch and receiver before depending on it in real sessions.

## Features

- Reads live heart-rate data from Health Services.
- Broadcasts over standard BLE heart-rate GATT UUIDs.
- Runs as a foreground service so broadcasting can continue with the screen off.
- Shows service state directly on-watch.
- Stores no workout or health history.

## Requirements

- A Wear OS watch running Android API 30 or newer.
- A built-in heart-rate sensor.
- BLE peripheral advertising support.
- Bluetooth enabled on the watch.

## Permissions

HrmApp requests:

- `BODY_SENSORS`
- `BODY_SENSORS_BACKGROUND`
- `BLUETOOTH_ADVERTISE`
- `BLUETOOTH_CONNECT`
- `POST_NOTIFICATIONS` on newer Android versions
- Foreground service permissions for health and connected-device use

Important:

- On Android 13 / API 33 and newer, background body-sensor access is not granted from the normal runtime dialog.
- The app will send you to system Settings so you can allow background sensor access there.

## Install

### From source

1. Clone the repo:

```bash
git clone https://github.com/StanleyMasinde/HrmApp.git
cd HrmApp
```

2. Open the project in Android Studio.
3. Connect a Wear OS device or use Wear OS deployment from Android Studio.
4. Build and install the `app` module.

### Local build

The project builds with Gradle and Android Studio's bundled JDK.

```bash
./gradlew assembleDebug
```

## Usage

1. Open HrmApp on the watch.
2. Grant the requested permissions.
3. If prompted, open app Settings and enable background sensor access.
4. Tap `Broadcast HR`.
5. Wait for the app to show heart-rate activity.
6. Pair from your receiver as a standard heart-rate monitor.

If the app shows `Check watch fit and sensor access`, make sure the watch is being worn correctly and that body-sensor permissions are fully granted.

## Compatibility Notes

- The app is intended for Wear OS watches with Android Health Services support.
- BLE receiver compatibility depends on the receiver accepting the standard Heart Rate Service.
- This project does not currently claim certification with any specific training platform or hardware vendor.

## Development

- Language: Kotlin
- UI: Jetpack Compose for Wear OS
- Sensor API: Android Health Services `MeasureClient`
- BLE profile: Heart Rate Service (`0x180D`)

Useful commands:

```bash
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for setup, testing expectations, and bug report details.

## License

Released under the MIT License. See [LICENSE](LICENSE).
