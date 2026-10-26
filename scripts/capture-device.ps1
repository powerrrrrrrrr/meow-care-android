param(
    [string]$Serial = 'emulator-5554',
    [ValidatePattern('^[a-z0-9-]+$')][string]$Name = 'today',
    [string]$AdbPath
)
$ErrorActionPreference = 'Stop'
if (-not $AdbPath) {
    $candidates = @('D:\Program Files\AndroidSDK\platform-tools\adb.exe', "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
    $AdbPath = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}
if (-not $AdbPath) { throw 'Pass -AdbPath with the path to adb.exe.' }
$projectDir = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $projectDir 'captures'
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$remoteImage = "/sdcard/meow-care-$Name.png"
$remoteXml = "/sdcard/meow-care-$Name.xml"
& $AdbPath -s $Serial shell screencap -p $remoteImage
if ($LASTEXITCODE -ne 0) { throw 'Device screenshot failed.' }
& $AdbPath -s $Serial pull $remoteImage (Join-Path $destination "$Name.png")
if ($LASTEXITCODE -ne 0) { throw 'Screenshot transfer failed.' }
& $AdbPath -s $Serial shell uiautomator dump $remoteXml
if ($LASTEXITCODE -ne 0) { throw 'Device hierarchy capture failed.' }
& $AdbPath -s $Serial pull $remoteXml (Join-Path $destination "$Name.xml")
if ($LASTEXITCODE -ne 0) { throw 'Hierarchy transfer failed.' }
Write-Output (Join-Path $destination "$Name.png")
