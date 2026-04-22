# 重邮校园网 Android 客户端 (CQUPT Net App)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Repository](https://img.shields.io/badge/GitHub-cqupt--net--app-blue?logo=github)](https://github.com/MeTerminator/cqupt-net-app)

一个使用 Kotlin 和 Jetpack Compose 编写的重邮（CQUPT）校园网认证客户端。

## ✨ 特性

- **现代化 UI**：采用 Jetpack Compose 构建，支持深色模式与玻璃拟态设计。
- **一键认证**：内置 Dr.COM 协议，支持一键登录、登出和状态刷新。
- **智能检测**：自动获取 IPv4 地址，实时显示当前连接状态。
- **快速配置**：简洁的账号配置界面，支持保存账号、密码及运营商选择。

## 🛠️ 编译与运行

### 环境要求
- Android SDK 35+
- Gradle 8.9+
- JDK 17+

### 编译 APK
在项目根目录下执行：
```bash
./gradlew :app:assembleDebug
```

### 安装到设备
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📂 项目结构

- `app/src/main/java/top/met6/cquptnet/MainActivity.kt`: UI 交互与 Compose 组件。
- `app/src/main/java/top/met6/cquptnet/SDK.kt`: 校园网认证核心逻辑。
- `app/src/main/java/top/met6/cquptnet/SettingsManager.kt`: 用户凭据持久化。

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 协议开源。

Copyright (c) 2026 MeTerminator
