# BravePrune

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

English | [简体中文](README.zh-CN.md)

This module provides privacy hardening and feature pruning for Brave.
It has no launcher UI. The default switch for the language-fingerprinting
protection sub-setting is injected at the end of Brave's **Settings > Shields &
Privacy** page.

## Requirements

- Android 13+ and an LSPosed framework with minimum API 102 support
- Brave Stable, Beta, Dev, Nightly, or Default channel

## Features

The following defaults are written once on first launch and remain
user-editable:

| Setting | Default |
| --- | --- |
| Fingerprinting protection | Strict |
| HTTPS upgrade | Strict |
| Shields tracker/ad blocking | Aggressive |
| WebRTC IP handling | Disable non-proxied UDP |
| Language-fingerprinting protection sub-setting | Off |

The module also displays built-in ad-block subscriptions that are hidden by
default, and enables the Strict fingerprinting option, first-party network
blocking, timestamp rounding, and HTTPS-by-default.

The following features are continuously disabled:

- Brave Ads, Rewards, News, and AI/Leo;
- Brave VPN, including blocking WireGuard service starts;
- telemetry, crash/metrics reporting, P3A, stats, and Web Discovery;
- Wallet/Web3, provider callbacks, Web3 notifications, and decentralized DNS.

Some features are disabled through Brave's Android policies, so the **managed
browser** indicator is expected. Restart Brave after changing the
language-fingerprinting protection switch.

## Implementation

- Hooks are installed after Brave's isolated `split_chrome` ClassLoader is
  ready.
- Feature parameters are merged before Chromium initializes `FeatureList`.
- Managed policies and preference guards disable pruned features.
- Defaults are written after Profile creation, and `PrefService` is captured to
  remain compatible with R8.

## Build and Release

This project currently builds with JDK 21 and Android SDK Platform 35.
The example below assumes that these tools are provided through local symlinks:

```bash
  env JAVA_HOME="$PWD/jdk" \
  ANDROID_HOME="$PWD/android-sdk" \
  ./gradlew testDebugUnitTest lintDebug assembleDebug
```

The current Release build uses a test signature. Configure a signing key before
distributing your own builds.

## Compatibility Checks

Prepare a local symlink to the Brave upstream source before running the
compatibility scripts. Check the linked Brave source and a released Brave APK:

```bash
  ./scripts/check-brave-compat.sh
  env ANDROID_HOME="$PWD/android-sdk" \
  ./scripts/check-brave-apk-compat.sh /path/to/brave.apk
```

If an update produces a `Critical compatibility adapter missing` error, a
required Brave runtime structure changed and must be adapted again.

## References

- [LibXposed API](https://libxposed.github.io/api/)
- [Develop Xposed Modules Using the Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
