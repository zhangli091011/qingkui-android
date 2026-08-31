[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Apk,
    [Parameter(Mandatory = $true)]
    [ValidateSet("pilot", "production")]
    [string]$Environment,
    [Parameter(Mandatory = $true)]
    [string]$BackendCommit,
    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[0-9a-fA-F]{64}$")]
    [string]$VectorIndexSha256,
    [Parameter(Mandatory = $true)]
    [string]$ApiRollbackImage,
    [string]$Output = "release-evidence/android-release-manifest.json"
)

$ErrorActionPreference = "Stop"
$apkPath = (Resolve-Path -LiteralPath $Apk).Path
$metadataPath = Join-Path (Split-Path -Parent $apkPath) "output-metadata.json"
if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
    throw "Gradle output metadata is missing: $metadataPath"
}
$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
$element = @($metadata.elements) | Where-Object { $_.outputFile -eq (Split-Path -Leaf $apkPath) } | Select-Object -First 1
if ($null -eq $element) {
    throw "APK is not listed in output-metadata.json: $apkPath"
}

$apksigner = Get-Command apksigner -ErrorAction SilentlyContinue
if ($null -eq $apksigner -and -not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    $apksigner = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_HOME "build-tools") -Filter "apksigner.bat" -Recurse |
        Sort-Object FullName -Descending |
        Select-Object -First 1
}
if ($null -eq $apksigner) {
    throw "apksigner was not found. Add Android build-tools to PATH or set ANDROID_HOME."
}
$apksignerPath = if ($apksigner.Path) { $apksigner.Path } else { $apksigner.FullName }
$verification = & $apksignerPath verify --verbose --print-certs $apkPath 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed."
}
$certificateLine = $verification | Where-Object { $_ -match "certificate SHA-256 digest:\s*([0-9a-fA-F]{64})" } | Select-Object -First 1
if ($null -eq $certificateLine) {
    throw "apksigner did not return a certificate SHA-256 digest."
}
$certificateLine -match "certificate SHA-256 digest:\s*([0-9a-fA-F]{64})" | Out-Null
$certificateSha256 = $Matches[1].ToLowerInvariant()
$apkSha256 = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
$androidCommit = (git -C (Split-Path -Parent $PSScriptRoot) rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $androidCommit -notmatch "^[0-9a-f]{40}$") {
    throw "Unable to resolve the Android Git commit."
}

$manifest = [ordered]@{
    schema = "qingkui-android-release-v1"
    status = "draft"
    created_at = [DateTimeOffset]::UtcNow.ToString("o")
    environment = $Environment
    android_commit = $androidCommit
    backend_commit = $BackendCommit
    version_code = [int]$element.versionCode
    version_name = [string]$element.versionName
    certificate_sha256 = $certificateSha256
    apk_sha256 = $apkSha256
    vector_index_sha256 = $VectorIndexSha256.ToLowerInvariant()
    api_rollback_image = $ApiRollbackImage
    approved_by = ""
    approved_at = $null
}
$outputPath = [System.IO.Path]::GetFullPath($Output)
$outputParent = Split-Path -Parent $outputPath
New-Item -ItemType Directory -Path $outputParent -Force | Out-Null
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $outputPath -Encoding utf8
Write-Output "Draft release manifest created: $outputPath"
Write-Output "APK SHA-256: $apkSha256"
Write-Output "Certificate SHA-256: $certificateSha256"
Write-Output "A release approver must independently verify the artifact and set status/approved_by/approved_at."
