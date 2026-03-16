$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$excludePattern = '\\.git\\|\\target\\|\\.idea\\|node_modules\\'

$bomFiles = Get-ChildItem -Path $root -Recurse -File |
    Where-Object { $_.FullName -notmatch $excludePattern } |
    Where-Object {
        $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
        $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
    } |
    Select-Object -ExpandProperty FullName

if ($bomFiles.Count -gt 0) {
    Write-Host "Found UTF-8 BOM in these files:" -ForegroundColor Red
    $bomFiles | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "OK: no UTF-8 BOM found." -ForegroundColor Green

