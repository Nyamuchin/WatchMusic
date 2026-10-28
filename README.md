# WatchMusic

为 Android 手表打造的本地音乐播放器，Material 3 Expressive 风格，界面全部中文。

## 功能

- **首屏播放页**：全屏专辑封面 + 渐变遮罩，曲名/歌手/红心，波浪形进度条（可拖动），上一首 / 播放暂停 / 下一首；顶部居中是播放选项与播放队列两个按钮。
- **曲库页**：全部歌曲、专辑、艺术家、播放列表、收藏、最近播放、搜索、设置（搜索与设置在标题右侧居中）。在播放页向左滑动，曲库页跟随手指从右侧滑入覆盖播放页；**滑过屏宽 1/4 松手即展开**，不足 1/4 自动回弹。在曲库页向右滑动（或按返回键）收起，回到播放页。
- **播放**：Media3 ExoPlayer + `MediaSessionService`，支持后台播放、系统媒体通知、蓝牙/耳机线控与锁屏（媒体会话）控制、来电/拔出耳机自动暂停。
- **队列与随机/循环**：队列弹层可跳转任意曲目；随机播放与循环（关/全部/单曲）；下一首播放、加入队列。退出应用再打开会恢复上次的播放队列与当前曲目位置（暂停状态）。
- **播放列表**：新建、重命名、删除，添加/移除歌曲（多选添加）。
- **搜索**：曲目、专辑、艺术家分组结果。
- **睡眠定时**：关 / 15 / 30 / 45 / 60 分钟，倒计时显示，到点暂停。
- **音量**：表冠旋转或弹层内 ± 按钮，系统音量 HUD。
- **封面取色**：从封面提取强调色，整个界面跟随（可在设置关闭）。
- **设置**：跟随封面取色、界面缩放（25% / 50% / 75% / 100% / 125% / 150% / 175% / 200%）、表冠调音量、触感反馈、曲库文件夹、重新扫描曲库、通知权限、关于。
- **界面缩放**：整屏等比缩放（25%–200%，25% 一档），播放选项 / 播放队列 / 歌曲操作等底部弹层同样跟随缩放。设置页与授权页始终按 100% 渲染，缩小后仍能改回来；放大到 150% 以上时，播放页底部的控制区会自动收窄以免被裁掉，列表长标题会省略。
- **沉浸式**：隐藏状态栏与导航栏，边缘上滑可临时唤出系统栏。

音乐来源只有设备本地媒体库（MediaStore），不内置任何演示音频或封面。
设置里的「曲库文件夹」用系统文件夹选择器（SAF）指定一个目录后，应用只读取该目录（含子目录）中的音频，其它位置的文件一律不读取，也不需要再申请音频读取权限；选择「恢复为整个媒体库」即可回到 MediaStore 全库。如果之前授权的文件夹后来不可读（授权被撤销、存储重挂载），应用会自动清掉该设置并回到整个媒体库，不会卡在报错页。
无论哪种模式，曲名 / 歌手 / 专辑都优先取音频文件**内嵌的元数据**（mp3 的 ID3、flac/ogg 的 Vorbis comment 等，通过 `MediaMetadataRetriever` 读取），读不到才退回 MediaStore 的值，再退回文件名的名称部分。

## 构建

需要 JDK 21 与 Android SDK；`local.properties`（`sdk.dir=...`）按本机 SDK 路径自己创建，它已被 `.gitignore` 排除。

```powershell
.\gradlew.bat :app:assembleDebug
# 单元测试
.\gradlew.bat :app:testDebugUnitTest
# 发行版（有签名配置时签名，R8 优化开启）
.\gradlew.bat :app:assembleRelease
```

## 技术栈

- Kotlin 2.2.10、AGP 9.4.1、Gradle 9.6、minSdk 26 / targetSdk 37
- Compose BOM 2026.09.00 + Material3 1.5.0-beta02（`MaterialExpressiveTheme`、波浪进度指示器、Expressive 动效）
- Media3 1.11.1（ExoPlayer / MediaSession）
- 无第三方 DI 与导航库：`AppRoot` 内手写路由栈 + 跟手的覆盖式曲库面板（拖动进度驱动位移动画）

## 代码结构

```
app/src/main/java/com/watchmusic/
├── MainActivity.kt          # 沉浸式入口
├── WatchMusicApp.kt         # Application + 手写容器（prefs/repository/artwork/volume）
├── data/                    # Models、MediaStore 仓库、SharedPreferences、JSON、取色
├── player/                  # MusicService、PlayerViewModel、状态、睡眠定时
├── ui/                      # AppRoot 与各屏幕、弹层、通用组件、主题
└── util/                    # 时间格式化、触感
```
