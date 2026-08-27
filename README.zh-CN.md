# BravePrune

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

[English](README.md) | 简体中文

本模块用于对 Brave 浏览器进行隐私强化与精简。
模块无启动界面，语言-指纹保护子项默认开关会注入到 Brave **设置 > 防护与隐私**页面末尾。

## 运行要求

- Android 13+，以及最低支持 API 102 的 LSPosed 框架
- Brave 正式版、Beta、Dev、Nightly 或 Default 渠道

## 功能

以下默认值首次启动写入一次，之后可由用户修改：

| 设置                    | 默认值           |
| ----------------------- | ---------------- |
| 指纹保护                | 严格             |
| HTTPS 升级              | 严格             |
| Shields 跟踪器/广告拦截 | 激进             |
| WebRTC IP 处理          | 禁用无代理的 UDP |
| 语言-指纹保护子项       | 关闭             |

模块还会显示默认隐藏的内置广告过滤订阅；启用严格指纹保护可选项、第一方网络拦截、时间戳取整和 HTTPS-by-default。

以下功能会被持续禁用：

- Brave Ads、Rewards、News、AI/Leo；
- Brave VPN，包括阻止 WireGuard 服务启动；
- 遥测、崩溃/指标上报、P3A、统计和 Web Discovery；
- Wallet/Web3、Provider 回调、Web3 通知和去中心化 DNS。

部分功能通过 Brave Android 策略禁用，出现**受管理浏览器**标识属于正常现象。修改语言指纹保护开关后需要重启 Brave 生效。

## 实现方式

- 等待 Brave 隔离的 `split_chrome` ClassLoader 就绪后安装 Hook。
- 在 Chromium 初始化 `FeatureList` 前合并特性参数。
- 使用托管策略和首选项保护禁用精简功能。
- 在 Profile 创建后写入默认值，并捕获 `PrefService` 以兼容 R8。

## 构建与发布

本项目当前构建使用 JDK 21 和 Android SDK Platform 35。
本说明以软链接形式为示例：

```bash
  env JAVA_HOME="$PWD/jdk" \
  ANDROID_HOME="$PWD/android-sdk" \
  ./gradlew testDebugUnitTest lintDebug assembleDebug
```

当前 Release 使用测试签名；自行分发版本前需要配置签名密钥。

## 兼容检查

运行兼容检查脚本时请在项目中准备 Brave 上游源码软链接。
检查链接的 Brave 源码和发布的 Brave APK：

```bash
  ./scripts/check-brave-compat.sh
  env ANDROID_HOME="$PWD/android-sdk" \
  ./scripts/check-brave-apk-compat.sh /path/to/brave.apk
```

若更新后出现 `Critical compatibility adapter missing` 错误日志表示 Brave 的必需运行时结构已经变化，需要重新适配。

## 参考资料

- [LibXposed API](https://libxposed.github.io/api/)
- [使用现代 Xposed API 开发模块](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 发布。
