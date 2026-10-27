param([switch]$Install)

$ErrorActionPreference = 'Stop'
$studioJava = 'C:\Program Files\Android\Android Studio\jbr'
if (Test-Path "$studioJava\bin\java.exe") {
    $env:JAVA_HOME = $studioJava
}

Push-Location $PSScriptRoot
try {
    & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
    if ($Install) {
        & .\gradlew.bat :app:installDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Installation failed. Connect an Android device with USB debugging enabled.' }
        $sdkProperties = Get-Content .\local.properties | Where-Object { $_ -match '^sdk.dir=' }
        $sdkPath = ($sdkProperties -replace '^sdk.dir=', '') -replace '\\:', ':' -replace '\\\\', '\'
        & "$sdkPath\platform-tools\adb.exe" shell am start -n com.example.tagmind/.MainActivity
        if ($LASTEXITCODE -ne 0) { throw 'Could not launch TagMind.' }
    }
} finally {
    Pop-Location
}
