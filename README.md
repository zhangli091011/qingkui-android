# 青葵计划 Android

原生 Android 首版界面，技术栈为 Kotlin、Jetpack Compose 和 MVVM。

## 本地运行

1. 使用 Android Studio 打开本目录。
2. 等待 Gradle 同步完成。
3. 选择 API 27 或更高版本的设备，运行 `app`。

默认连接已部署的 HTTPS API：

- API：`https://qingkui-api.82-158-229-157.sslip.io/api/`
- 接口文档：`https://qingkui-api.82-158-229-157.sslip.io/docs`
- 健康检查：`https://qingkui-api.82-158-229-157.sslip.io/health`

使用 `QINGKUI_ENV` 在 `development`、`test`、`pilot`、`production` 四套配置间选择：

```powershell
.\gradlew.bat :app:installDebug -PQINGKUI_ENV=development
```

地址定义在 `config/environments/*.properties`。Debug 可连接本地 HTTP；Release 始终使用 pilot/production HTTPS，主清单不包含生产 HTTP 白名单。

命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug -PQINGKUI_ENV=production
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。CI 上传的
`qingkui-production-connected-debug-apk` 明确连接生产 HTTPS API，仅供内部设备验收；它仍使用 Debug
签名，不能投放到学生 MDM。正式试点包必须提供递增版本号和独立 keystore，并通过 `assembleRelease` 的签名门。

## 当前实现

- Figma 对齐的 1280x800 横屏与 412x915 竖屏响应式首页
- 顶部灵动岛导航与会话抽屉
- 跟随系统的完整浅色/深色主题，包括文字、表面、边框、控件与图谱色板
- 登录、注册、令牌持久化、退出登录与单次 401 刷新轮换
- 对接后端额度、知识节点邻接、学习摘要与 AI 问答引用
- 优先使用 SSE 流式问答并增量更新同一条消息；旧后端自动回退兼容
- 使用随 APK 打包的 MIT KaTeX 离线渲染行内与块级 LaTeX，深浅主题同步换色，异常公式回退原文
- 问答额度使用服务端原子返回的余额，不在客户端自行扣减
- 可缩放、拖拽、点选和按关系筛选的原生 Compose Canvas 知识图谱
- 图谱/知识卡双视图、节点来源标记、掌握状态与关联详情
- 收藏、掌握状态和私密笔记均调用真实接口，节点详情与列表状态保持同步
- 最近探索、待复习、易错与已验证学习列表
- 使用 SavedStateHandle 恢复未发送问题、当前会话、知识范围和学习筛选；错题上传继续由 Room/WorkManager 恢复
- 登录令牌由 Android Keystore 加密保存，并从旧版明文 DataStore 自动迁移；应用数据禁止云备份和设备迁移
- 账户、额度与设置界面
- 后端错误、额度不足、AI 未配置与登录过期均显示为明确的界面状态
