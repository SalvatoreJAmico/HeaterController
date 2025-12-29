param(
    [string]$Package = "com.salvatore.heatercontroller",
    [string]$Device = "",
    [string]$Pair = "",        # e.g. 192.168.1.50:34567 (pairing port)
    [string]$PairCode = "",    # 6-digit pairing code
    [string]$Connect = "",     # e.g. 192.168.1.50:5555 (adb tcp port)
    [string]$OutFile = ""      # optional: save logs to this file
)

$ErrorActionPreference = 'Stop'
$adb = Join-Path $Env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' }

& $adb start-server | Out-Null

if ($Pair -and $PairCode) {
    Write-Host "Pairing to $Pair ..."
    & $adb pair $Pair $PairCode
}

if ($Connect) {
    Write-Host "Connecting to $Connect ..."
    & $adb connect $Connect | Out-Null
}

$devs = (& $adb devices | Where-Object { $_ -match '^\S+\s+device$' }) | ForEach-Object { ($_.ToString().Split()[0]) }
if (-not $Device) {
    if ($devs.Count -eq 1) { $Device = $devs[0] }
    elseif ($devs.Count -gt 1) {
        Write-Warning "Multiple devices found: $($devs -join ', '). Re-run with -Device <serial>."
        exit 1
    } else {
        Write-Error "No devices connected. Enable USB/Wireless debugging and try again."
        exit 1
    }
}

Write-Host "Using device: $Device"

# Ensure app is running to obtain PID
$appPid = (& $adb -s $Device shell pidof $Package) -join ''
if (-not $appPid) {
    Write-Host "Launching $Package ..."
    & $adb -s $Device shell am start -n com.salvatore.heatercontroller/.MainActivity | Out-Null
    Start-Sleep -Seconds 2
    $appPid = (& $adb -s $Device shell pidof $Package) -join ''
}

if (-not $appPid) {
    Write-Error "Could not obtain PID for $Package. Open the app, then rerun."
    exit 1
}

Write-Host "PID: $appPid. Streaming app logs (Ctrl+C to stop) ..."
if ($OutFile) {
    Write-Host "Saving logs to $OutFile"
    & $adb -s $Device logcat --pid $appPid | Tee-Object -FilePath $OutFile
} else {
    & $adb -s $Device logcat --pid $appPid
}
