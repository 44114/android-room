<p align="center">
  <a href="#english">English</a> |
  <a href="#%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87">简体中文</a> |
  <a href="#%E7%B9%81%E9%AB%94%E4%B8%AD%E6%96%87">繁體中文</a>
</p>

---

<a id="english"></a>

# 💬 Chat Room — Android Client

> ⚠️ **Keep Android Open** — Starting September 2026, Google will silently block apps from unregistered developers on all Android devices. Sideloading freedom is under threat. Visit [keepandroidopen.org](https://keepandroidopen.org) to learn more and take action.

Android app for the [Chat Room](https://github.com/44114/room) real-time messaging server. Built with **Jetpack Compose**, **OkHttp**, and **Socket.IO**, supporting text messaging, file sharing (up to 4 GB), account management, and configurable server URL. Requires **Android 8.0 (API 26)** or later.

> 🤖 **Developed with assistance from [Claude Code](https://claude.ai/code) (Anthropic)**

---

## 📱 User Guide

### Installation

1. Download the latest APK from [Releases](https://github.com/44114/android-room/releases).
2. On your Android device, go to **Settings → Security → Install unknown apps** and allow your browser/file manager.
3. Open the downloaded APK and tap **Install**.

### First Launch

1. When you first open the app, you will see a **Server URL** screen.
2. Enter the address of your Chat Room server, for example:
   - Home network: `http://192.168.1.100:9888`
   - Public server: `https://chat.example.com`
   - Android Emulator: tap "使用模拟器默认地址" to use `http://10.0.2.2:9888`
3. Tap **Connect to Server**.

### Using the App

| Feature | How to Use |
|---------|-------------|
| **Register** | Tap "没有账号？立即注册" — enter username, password, invite code, complete Turnstile verification |
| **Login** | Enter your username and password on the login screen, optionally check "Remember Me" |
| **Send message** | Type in the bottom input field, tap the send button ✉️ or press Enter |
| **Send file** | Tap 📎, select a file from your device (max 4 GB) |
| **Download file** | Tap the ⬇ Download button on any file card in the chat |
| **Change password** | Navigate to ⚙️ Account → enter current + new password → save |
| **Delete account** | Navigate to ⚙️ Account → enter password → confirm deletion |
| **Change server** | Navigate to ⚙️ Account → tap "更换服务器" → enter new URL |

---

## 🛠 Developer Guide — Building from Source

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17**
- **Android SDK 35** (installed via Android Studio SDK Manager)
- **Gradle 8.11** (bundled via wrapper — no manual install needed)

### Clone & Open

```bash
git clone https://github.com/44114/android-room.git
cd android-room
```

Open the project in Android Studio. Gradle will sync automatically on first open.

### Configure the Server URL

The default server URL is set in `app/build.gradle.kts` for debug builds:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9888\"")
```

- `10.0.2.2` is the Android Emulator alias for the host machine's `localhost`.
- For a physical device on the same network, use your computer's LAN IP (e.g. `http://192.168.1.100:9888`).
- The app also allows users to override this at runtime via the Setup screen.

### Build APK

```bash
# Debug APK (faster, includes logging)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (optimized, minified)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

> For release builds, edit `app/build.gradle.kts` to set your production server URL (the `release` build type).

### Project Structure

```
android/app/src/main/java/com/chatroom/app/
├── ChatRoomApp.kt             # Application class (DI)
├── MainActivity.kt            # Single-activity Compose entry
├── data/
│   ├── api/
│   │   ├── ApiService.kt      # REST client (OkHttp + CookieJar)
│   │   └── SocketManager.kt   # WebSocket (Socket.IO)
│   ├── local/
│   │   └── PreferencesManager.kt  # DataStore preferences
│   ├── model/                 # Data classes
│   └── repository/            # Auth/Chat/File repositories
└── ui/
    ├── theme/                 # Material3 theme
    ├── navigation/NavGraph.kt # Navigation graph
    ├── setup/SetupScreen.kt   # Server URL configuration
    ├── auth/                  # Login + Register screens
    ├── chat/ChatScreen.kt     # Main chat interface
    ├── account/               # Account management
    └── components/            # MessageBubble, FileCard
```

### Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material3 |
| HTTP | OkHttp 4.12 + CookieJar |
| WebSocket | Socket.IO Java Client 2.1 |
| Serialization | Kotlinx Serialization |
| Persistence | DataStore Preferences |
| Image Loading | Coil Compose |
| Build | Gradle Kotlin DSL, AGP 8.7 |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<a id="简体中文"></a>

# 💬 聊天室 — Android 客户端

> ⚠️ **保卫开放的 Android** — Google 将于 2026 年 9 月起强制封锁未注册开发者的应用，侧载自由面临威胁。访问 [keepandroidopen.org](https://keepandroidopen.org) 了解详情并支持这项运动。

[Chat Room](https://github.com/44114/room) 即时通讯服务器的 Android 客户端。基于 **Jetpack Compose**、**OkHttp** 和 **Socket.IO** 构建，支持文字消息、文件分享（最大 4 GB）和账号管理。

> 🤖 **本项目由 [Claude Code](https://claude.ai/code) (Anthropic) 辅助开发**

---

## 📱 用户使用指南

### 安装

1. 从 [Releases](https://github.com/44114/android-room/releases) 页面下载最新 APK。
2. 在 Android 设备上前往 **设置 → 安全 → 安装未知应用**，允许您的浏览器/文件管理器。
3. 打开下载的 APK，点击 **安装**。

### 首次启动

1. 首次打开应用时会看到 **服务器地址** 界面。
2. 输入聊天室服务器的地址，例如：
   - 家庭网络：`http://192.168.1.100:9888`
   - 公网服务器：`https://chat.example.com`
   - Android 模拟器：点击"使用模拟器默认地址"使用 `http://10.0.2.2:9888`
3. 点击 **连接服务器**。

### 功能说明

| 功能 | 操作方式 |
|------|---------|
| **注册账号** | 点击"没有账号？立即注册"→ 输入用户名、密码、邀请码，完成人机验证 |
| **登录** | 输入用户名和密码，可选勾选"记住我" |
| **发送消息** | 在底部输入框输入文字，点击发送按钮 ✉️ |
| **发送文件** | 点击 📎，从设备中选择文件（最大 4 GB） |
| **下载文件** | 点击聊天中文件卡片上的 ⬇ 下载按钮 |
| **修改密码** | 进入 ⚙️ 账号管理 → 输入当前密码和新密码 → 保存 |
| **注销账号** | 进入 ⚙️ 账号管理 → 输入密码 → 确认注销 |
| **更换服务器** | 进入 ⚙️ 账号管理 → 点击"更换服务器"→ 输入新地址 |

---

## 🛠 开发者指南 — 从源码构建

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更新版本
- **JDK 17**
- **Android SDK 35**（通过 Android Studio SDK Manager 安装）
- **Gradle 8.11**（通过 wrapper 自动下载，无需手动安装）

### 克隆并打开

```bash
git clone https://github.com/44114/android-room.git
cd android-room
```

在 Android Studio 中打开项目，Gradle 将自动同步。

### 配置服务器地址

默认服务器地址在 `app/build.gradle.kts` 中设置：

```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9888\"")
```

- `10.0.2.2` 是 Android 模拟器中宿主机 `localhost` 的别名。
- 同一网络下的真机测试，请使用电脑的局域网 IP（如 `http://192.168.1.100:9888`）。
- 用户也可以在应用内的设置界面自行修改服务器地址。

### 构建 APK

```bash
# Debug APK（快速，含日志）
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# Release APK（优化、混淆、压缩）
./gradlew assembleRelease
# 输出: app/build/outputs/apk/release/app-release.apk
```

> Release 构建前，请在 `app/build.gradle.kts` 中修改 `release` 构建类型的 `BASE_URL` 为生产环境服务器地址。

### 项目结构

```
android/app/src/main/java/com/chatroom/app/
├── ChatRoomApp.kt             # Application 类（依赖注入）
├── MainActivity.kt            # 单 Activity Compose 入口
├── data/
│   ├── api/
│   │   ├── ApiService.kt      # REST 客户端
│   │   └── SocketManager.kt   # WebSocket 管理
│   ├── local/
│   │   └── PreferencesManager.kt  # DataStore 存储
│   ├── model/                 # 数据模型
│   └── repository/            # 仓库层
└── ui/
    ├── theme/                 # Material3 主题
    ├── navigation/NavGraph.kt # 导航图
    ├── setup/SetupScreen.kt   # 服务器地址配置
    ├── auth/                  # 登录 + 注册
    ├── chat/ChatScreen.kt     # 聊天主界面
    ├── account/               # 账号管理
    └── components/            # 消息气泡、文件卡片
```

### 技术栈

| 层级 | 库 |
|------|-----|
| UI | Jetpack Compose + Material3 |
| HTTP | OkHttp 4.12 + CookieJar |
| WebSocket | Socket.IO Java Client 2.1 |
| 序列化 | Kotlinx Serialization |
| 持久化 | DataStore Preferences |
| 图片 | Coil Compose |
| 构建 | Gradle Kotlin DSL, AGP 8.7 |

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 授权。

---

<a id="繁體中文"></a>

# 💬 聊天室 — Android 用戶端

> ⚠️ **守護開放的 Android** — Google 將於 2026 年 9 月起強制封鎖未註冊開發者的應用，側載自由面臨威脅。前往 [keepandroidopen.org](https://keepandroidopen.org) 了解更多並支持這項運動。

[Chat Room](https://github.com/44114/room) 即時通訊伺服器的 Android 用戶端。基於 **Jetpack Compose**、**OkHttp** 和 **Socket.IO** 建構，支援文字訊息、檔案分享（最大 4 GB）和帳號管理。

> 🤖 **本專案由 [Claude Code](https://claude.ai/code) (Anthropic) 輔助開發**

---

## 📱 使用者指南

### 安裝

1. 從 [Releases](https://github.com/44114/android-room/releases) 頁面下載最新 APK。
2. 在 Android 裝置上前往 **設定 → 安全性 → 安裝未知應用程式**，允許您的瀏覽器/檔案管理器。
3. 開啟下載的 APK，點選 **安裝**。

### 首次啟動

1. 首次開啟應用程式時會看到 **伺服器位址** 畫面。
2. 輸入聊天室伺服器的位址，例如：
   - 家庭網路：`http://192.168.1.100:9888`
   - 公開伺服器：`https://chat.example.com`
   - Android 模擬器：點選「使用模擬器預設位址」使用 `http://10.0.2.2:9888`
3. 點選 **連線伺服器**。

### 功能說明

| 功能 | 操作方式 |
|------|---------|
| **註冊帳號** | 點選「沒有帳號？立即註冊」→ 輸入使用者名稱、密碼、邀請碼，完成人機驗證 |
| **登入** | 輸入使用者名稱和密碼，可選勾選「記住我」 |
| **傳送訊息** | 在底部輸入框輸入文字，點選傳送按鈕 ✉️ |
| **傳送檔案** | 點選 📎，從裝置中選擇檔案（最大 4 GB） |
| **下載檔案** | 點選聊天中檔案卡片上的 ⬇ 下載按鈕 |
| **修改密碼** | 進入 ⚙️ 帳號管理 → 輸入目前密碼和新密碼 → 儲存 |
| **註銷帳號** | 進入 ⚙️ 帳號管理 → 輸入密碼 → 確認註銷 |
| **更換伺服器** | 進入 ⚙️ 帳號管理 → 點選「更換伺服器」→ 輸入新位址 |

---

## 🛠 開發者指南 — 從原始碼建構

### 環境要求

- **Android Studio** Hedgehog (2023.1.1) 或更新版本
- **JDK 17**
- **Android SDK 35**（透過 Android Studio SDK Manager 安裝）
- **Gradle 8.11**（透過 wrapper 自動下載，無需手動安裝）

### 複製並開啟

```bash
git clone https://github.com/44114/android-room.git
cd android-room
```

在 Android Studio 中開啟專案，Gradle 將自動同步。

### 建構 APK

```bash
# Debug APK（快速，含日誌）
./gradlew assembleDebug
# 輸出: app/build/outputs/apk/debug/app-debug.apk

# Release APK（優化、混淆、壓縮）
./gradlew assembleRelease
# 輸出: app/build/outputs/apk/release/app-release.apk
```

> Release 建構前，請在 `app/build.gradle.kts` 中修改 `release` 建構類型的 `BASE_URL` 為生產環境伺服器位址。

### 技術棧

| 層級 | 函式庫 |
|------|--------|
| UI | Jetpack Compose + Material3 |
| HTTP | OkHttp 4.12 + CookieJar |
| WebSocket | Socket.IO Java Client 2.1 |
| 序列化 | Kotlinx Serialization |
| 持久化 | DataStore Preferences |
| 圖片 | Coil Compose |
| 建構 | Gradle Kotlin DSL, AGP 8.7 |

---

## 📄 授權條款

本專案採用 [MIT License](LICENSE) 授權。

---

<p align="center">
  <sub>🤖 Developed with <a href="https://claude.ai/code">Claude Code</a> (Anthropic) · 2026</sub>
</p>
