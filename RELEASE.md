# Android 签名发布门

正式 APK 只能从 `pilot` 或 `production` 环境构建。Gradle 不提供默认正式签名；缺少任一签名参数或显式版本号时，`assembleRelease` 会在打包前失败。

## 输入

将 keystore 放在仓库外的受控目录，并通过当前终端或 CI Secret 注入：

```powershell
$env:QINGKUI_SIGNING_STORE_FILE = "D:\secure\qingkui-release.p12"
$env:QINGKUI_SIGNING_STORE_PASSWORD = "<secret>"
$env:QINGKUI_SIGNING_KEY_ALIAS = "qingkui-upload"
$env:QINGKUI_SIGNING_KEY_PASSWORD = "<secret>"
```

不要把密码写入 `gradle.properties`、命令历史、构建日志或 MDM 描述。仓库已忽略 `*.jks`、`*.keystore`、`keystore.properties` 和 `.env*`，但正式 keystore 仍不得放入工作区。

## 构建与核验

每次发布必须显式递增版本号：

```powershell
.\gradlew.bat :app:clean :app:assembleRelease `
  "-PQINGKUI_ENV=pilot" `
  "-PQINGKUI_VERSION_CODE=2" `
  "-PQINGKUI_VERSION_NAME=0.1.1"
```

切换正式环境时使用 `"-PQINGKUI_ENV=production"`。PowerShell 下应保留参数引号，避免带点号的版本名被拆成 Gradle 任务。构建后执行：

```powershell
$apk = "app\build\outputs\apk\release\app-release.apk"
apksigner verify --verbose --print-certs $apk
Get-FileHash -Algorithm SHA256 $apk
```

发布记录必须包含 Git 提交、环境、`versionCode`、`versionName`、证书 SHA-256、APK SHA-256、向量索引 SHA-256 和 API 回滚镜像。Debug APK 不得进入学生 MDM 应用目录。

使用脚本从 Gradle 元数据和已签名 APK 生成不可误认成正式批准的草稿清单：

```powershell
.\scripts\New-ReleaseManifest.ps1 `
  -Apk app\build\outputs\apk\release\app-release.apk `
  -Environment pilot `
  -BackendCommit <40位提交哈希> `
  -VectorIndexSha256 <64位索引哈希> `
  -ApiRollbackImage <上一批准镜像摘要> `
  -Output release-evidence\android-release-manifest.json
```

脚本会调用 `apksigner verify`，读取真实 `versionCode/versionName`，计算证书和 APK SHA-256。输出始终为 `status=draft`；发布负责人独立核验后再填写 `status=approved`、`approved_by` 和 `approved_at`，并交由后端统一发布就绪报告校验。

## 回滚

Android 不允许直接安装较低 `versionCode`。紧急回滚包应使用上一批准代码和同一签名证书，但分配更高的 `versionCode`，先进入内部设备组验证，再分阶段推送试点组。
