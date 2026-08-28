# 青葵计划 Android

原生 Android 首版界面，技术栈为 Kotlin、Jetpack Compose 和 MVVM。

## 本地运行

1. 使用 Android Studio 打开本目录。
2. 等待 Gradle 同步完成。
3. 选择 API 27 或更高版本的设备，运行 `app`。

命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 当前实现

- Figma 对齐的 1280x800 横屏与 412x915 竖屏响应式首页
- 顶部灵动岛导航与会话抽屉
- 可输入、发送并展示来源/反馈的本地问答流程
- 可缩放、拖拽、点选的原生 Compose Canvas 知识图谱
- 最近探索、待复习、易错与已验证学习列表
- 账户、额度与设置界面
- `SavedStateHandle` 保存当前页面和问答草稿

后端已独立维护在 [qingkui-backend](https://github.com/zhangli091011/qingkui-backend)。客户端接入时按后端仓库的 `ANDROID_INTEGRATION.md` 增加 repository/API 层，替换 `AppViewModel` 内的本地演示数据。
