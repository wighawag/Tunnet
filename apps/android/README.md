# Tunnet Lite (Android)

A small Jetpack Compose app for joining a Direct mesh from a phone: status, mesh IP, peers with per-peer ping, join by invite code, connect and disconnect. No React Native.

It shares its native layer with `apps/mobile`: the `:native` module compiles `apps/mobile/modules/tunnet-native/android/src/main/java/io/tunnet/android` (the `VpnService` host, JNI facade, Keystore, LAN and multicast handling) and its tests directly, leaving out only the Expo bridge. Changes there apply here without copying.

The application id is `io.tunnet.android.lite`, so it installs alongside the Expo app (`io.tunnet.android`). Android runs one VPN at a time.

## Prerequisites

- JDK 17 or newer
- Android SDK with `platforms;android-37.0` and NDK `30.0.16248370`
- `cargo-ndk` 4.1.2 (`cargo install cargo-ndk --version 4.1.2 --locked`)
- Rust targets `aarch64-linux-android` and `x86_64-linux-android` for the pinned toolchain

Set `ANDROID_HOME` or `sdk.dir` in `local.properties`.

## Build and install

```sh
cd apps/android
./gradlew :app:assembleDebug -Ptunnet.abis=arm64-v8a   # phone only; omit for arm64 + x86_64
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The agent is always built with the Rust release profile: an unstripped debug agent is about 800 MB per ABI. Expect several minutes per ABI for a cold Rust build.

Tests: `./gradlew :native:testDebugUnitTest :app:testDebugUnitTest`.

## Use

1. On a machine in the mesh: `tunnet invite <network>`.
2. Paste the code into the app and tap **Start and join**. Grant VPN consent, and local network access if asked (without it, peers are reached through relays only).
3. Once connected, peers appear with their state and path. **Ping** sends ICMP over the tunnel.

Always-on VPN works through Android settings. An explicit Disconnect stays disconnected.
