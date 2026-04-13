<h1 align="center">灵感胶囊</h1>
<p align="center">毫秒级记录灵感，无需打开手机，记录每一刻灵感，让灵感永不消逝</p>



`灵感胶囊` 是一个基于 Android 原生开发的灵感记录应用，主打“快速捕捉 + AI 提炼 + 卡片化浏览”。你可以通过主界面录音、手动输入、桌面小组件，甚至在亮屏或锁屏状态下三击音量上键，快速把一闪而过的想法保存下来，并交给 Dify Agent 进一步整理成更适合回看的灵感内容。

## 功能亮点

- 语音录音转灵感：主界面可一键开始/停止录音，录音结束后自动调用 AI 处理。
- 文本输入双路径：支持直接保存原始文本，也支持先交给 AI 提炼后再保存。
- 桌面录音小组件：在桌面快速开始录音、停止录音，并显示最近一次保存的灵感内容。
- 桌面文本输入小组件：从桌面直接唤起输入弹窗，快速记下想法。
- 音量键快捷录音：开启无障碍服务后，可在亮屏或锁屏状态下三击音量上键触发录音。
- 灵感悬浮窗：把某条灵感以悬浮窗形式固定到屏幕上，便于随时查看。
- 卡片式浏览：通过 `ViewPager2` 实现 3D 卡片流效果浏览灵感内容。
- 本地持久化：所有灵感内容保存在本地 SQLite 数据库中。
- 录音保护机制：主界面录音最长 60 秒，超时会自动停止，避免误触后长时间占用麦克风。

## 主要流程

### 语音路径

1. 录音生成 `.m4a` 文件
2. 调用 Dify `audio-to-text` 接口完成转写
3. 将转写结果发送到 Dify `chat-messages` 接口
4. 解析流式返回内容并保存为灵感卡片

### 文本路径

1. 用户输入文本
2. 选择“直接保存”或“AI 提炼”
3. 若走 AI 路径，则调用 Dify `chat-messages`
4. 保存结果到本地数据库并刷新主界面

## 技术栈

- Kotlin
- Android SDK / AppCompat
- ViewBinding
- ViewModel + LiveData
- Kotlin Coroutines
- OkHttp
- Gson
- SQLiteOpenHelper
- App Widget
- Accessibility Service
- Foreground Service

## 运行环境

- Android Studio，建议使用较新的稳定版
- JDK 17
- Android SDK 34
- `minSdk 26`
- `targetSdk 34`
- Gradle Android Plugin `8.2.0`
- Kotlin `1.9.0`

## 项目结构

```text
.
├─ APK/                                  # 已打包 APK 与更新日志
├─ app/
│  ├─ src/main/java/com/lgjn/inspirationcapsule/
│  │  ├─ MainActivity.kt                 # 主界面，录音、卡片浏览、编辑、删除、悬浮窗入口
│  │  ├─ TextInputActivity.kt            # 文本输入弹窗承载页
│  │  ├─ RecordingOverlayActivity.kt     # 旧版透明录音页，保留兼容
│  │  ├─ adapter/
│  │  ├─ api/
│  │  │  └─ DifyApiService.kt            # Dify 接口调用与 SSE 解析
│  │  ├─ data/
│  │  │  ├─ Inspiration.kt
│  │  │  ├─ InspirationDbHelper.kt       # SQLite 表结构与 CRUD
│  │  │  └─ InspirationRepository.kt
│  │  ├─ service/
│  │  │  ├─ FloatingWindowService.kt
│  │  │  ├─ RecordingService.kt          # 旧版录音服务，保留兼容
│  │  │  ├─ RecordingWidgetService.kt    # 小组件录音、AI 处理、通知与状态同步
│  │  │  └─ VolumeKeyAccessibilityService.kt
│  │  ├─ viewmodel/
│  │  └─ widget/
│  ├─ src/main/res/                      # 布局、图标、小组件与主题资源
│  └─ build.gradle
├─ build.gradle
├─ gradle.properties
├─ gradlew
└─ settings.gradle
```

## 快速开始

### 1. 打开项目

直接使用 Android Studio 打开仓库根目录即可。

### 2. 准备 Android 环境

确保本机已安装：

- Android SDK 34
- JDK 17
- 对应版本的 Build Tools / Platform Tools

项目中的 `local.properties` 使用的是本地 SDK 路径示例：

```properties
sdk.dir=C\:\\Users\\XYKing\\AppData\\Local\\Android\\Sdk
```

如果你的 SDK 路径不同，请按本机环境调整。

### 3. 配置 Dify

当前项目直接在 `app/src/main/java/com/lgjn/inspirationcapsule/api/DifyApiService.kt` 中写死了 Dify 配置。运行前请确认以下常量可用：

```kotlin
private const val API_KEY = "your-dify-app-key"
private const val BASE_URL = "https://api.dify.ai/v1"
private const val USER_ID = "your-user-id"
```

你需要准备一个可用的 Dify 应用，满足以下调用方式：

- 语音录音走 `/v1/audio-to-text`
- 文本提炼走 `/v1/chat-messages`
- 语音路径会先转写，再把转写结果交给 `chat-messages`

### 4. 同步并运行

优先推荐直接在 Android Studio 中：

- Sync Gradle
- 连接真机或启动模拟器
- 运行 `app`

如需命令行打包，可使用：

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## 使用说明

### 主界面

- 点击底部录音区域开始录音，再次点击停止录音并触发 AI 处理。
- 点击手动记录区域，打开文本输入弹窗。
- 长按某条灵感卡片可编辑内容。
- 点击卡片上的悬浮按钮，可把灵感显示为屏幕悬浮窗。
- 点击删除按钮，可移除对应灵感。

### 桌面小组件

- 录音小组件：适合快速语音捕捉，支持录音中、生成中、空闲三种状态。
- 文本输入小组件：适合随手记录一句话，再决定是直接保存还是 AI 提炼。

### 音量键快捷录音

开启无障碍服务后：

- 在约 600ms 窗口内连续三击音量上键
- 空闲状态下会开始录音
- 录音状态下会停止录音并进入 AI 处理
- 生成状态下会忽略触发

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `RECORD_AUDIO` | 录音 |
| `INTERNET` | 请求 Dify 接口 |
| `SYSTEM_ALERT_WINDOW` | 灵感悬浮窗与保存成功提示悬浮层 |
| `FOREGROUND_SERVICE` | 前台录音服务 |
| `FOREGROUND_SERVICE_MICROPHONE` | 前台麦克风录音 |
| `VIBRATE` | 三击音量键与保存成功振动反馈 |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | 旧系统兼容保留 |
| 无障碍服务 | 监听音量上键三击快捷操作 |

## 数据与隐私

- 灵感内容默认保存在本地 SQLite 数据库 `inspirations.db`。
- 录音文件保存在应用缓存目录 `cache/recordings/` 下。
- 语音文件在完成转写后会被删除，当前应用不提供录音回放功能。
- 用户输入的文本与语音转写结果会发送到 Dify 服务端进行处理。

如果你准备对外发布，建议在 README、隐私政策和应用内权限说明中明确告知用户网络处理链路与数据去向。

## 当前仓库中的注意点

- Dify 的 `API_KEY` 当前直接写在源码里，不适合公开仓库或多人协作环境。更推荐改为 `local.properties`、Gradle 配置或 `BuildConfig` 注入。
- `release` 构建目前使用的是 `debug` 签名配置，只适合本地测试，不适合正式发布。
- 仓库里存在 `gradlew`，但没有看到 `gradlew.bat`。如果你在 Windows 下走命令行打包，建议优先使用 Android Studio，或自行补齐对应的 wrapper 脚本。
- 仓库当前未包含自动化测试代码，功能验证主要依赖真机手测。
- `APK/` 目录下保留了历史安装包与更新日志，可用于回溯发布版本。

## 后续可优化方向

- 将 Dify 配置改为环境注入，避免密钥硬编码
- 补充 Room、Repository 测试与 UI 自动化测试
- 为灵感卡片增加搜索、分类、标签与导出能力
- 为录音小组件增加更清晰的状态反馈和异常提示
- 补充应用截图、架构图和发布说明

## 许可证

当前仓库未看到明确的 License 文件。如需开源发布，建议补充合适的许可证说明。
