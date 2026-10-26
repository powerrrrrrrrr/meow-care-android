param(
    [string[]]$Tasks = @('testDebugUnitTest', 'assembleDebug', 'lintDebug'),
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
if (-not $JavaHome) {
    $candidates = @('D:\Program Files\Android\Android Studio\jbr', 'C:\Program Files\Android\Android Studio\jbr')
    $JavaHome = $candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe') } | Select-Object -First 1
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) {
    throw 'Set JAVA_HOME to a compatible JDK (17 or 21), or pass -JavaHome.'
}
$oldJava = $env:JAVA_HOME
$oldTemp = $env:TEMP
$oldTmp = $env:TMP
Push-Location $projectDir
try {
    $env:JAVA_HOME = $JavaHome
    # A short, real Windows path avoids NIO Unix-domain pipe failures in packaged shells.
    $buildTemp = Join-Path $projectDir '.build-tmp'
    New-Item -ItemType Directory -Path $buildTemp -Force | Out-Null
    $env:TEMP = $buildTemp
    $env:TMP = $buildTemp
    $gradleArgs = @('--console=plain', '--no-daemon') + $Tasks
    if ($Offline) { $gradleArgs += '--offline' }
    & (Join-Path $projectDir 'gradlew.bat') @gradleArgs
    $buildExit = $LASTEXITCODE
    if ($buildExit -ne 0) { throw "Gradle failed with exit code $buildExit." }
} finally {
    $env:JAVA_HOME = $oldJava
    $env:TEMP = $oldTemp
    $env:TMP = $oldTmp
    Pop-Location
}
