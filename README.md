# 青葵计划 Android

原生 Android 首版界面，技术栈为 Kotlin、Jetpack Compose 和 MVVM。

## 本地运行

1. 使用 Android Studio 打开本目录。
2. 等待 Gradle 同步完成。
3. 选择 API 27 或更高版本的设备，运行 `app`。

默认连接已部署的 API：

- API：`http://82.158.229.157:8000/api/`
- 接口文档：`http://82.158.229.157:8000/docs`
- 健康检查：`http://82.158.229.157:8000/health`

如需切回本地模拟器后端，可在构建时覆盖地址：

```powershell
.\gradlew.bat :app:installDebug -PQINGKUI_API_BASE_URL=http://10.0.2.2:8000/api/
```

当前部署地址使用 HTTP，正式发布前建议配置 HTTPS。release 包仅对白名单中的部署地址允许明文请求。

命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 当前实现

- Figma 对齐的 1280x800 横屏与 412x915 竖屏响应式首页
- 顶部灵动岛导航与会话抽屉
- 跟随系统的完整浅色/深色主题，包括文字、表面、边框、控件与图谱色板
- 登录、注册、令牌持久化、退出登录与单次 401 刷新轮换
- 对接后端额度、知识节点邻接、学习摘要与 AI 问答引用
- 优先使用 SSE 流式问答并增量更新同一条消息；旧后端自动回退兼容
- 问答额度使用服务端原子返回的余额，不在客户端自行扣减
- 可缩放、拖拽、点选和按关系筛选的原生 Compose Canvas 知识图谱
- 图谱/知识卡双视图、节点来源标记、掌握状态与关联详情
- 最近探索、待复习、易错与已验证学习列表
- 账户、额度与设置界面
- 后端错误、额度不足、AI 未配置与登录过期均显示为明确的界面状态
