$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RootDir

if (-not $env:REACTOR_VM_EXECUTION_ENABLED) { $env:REACTOR_VM_EXECUTION_ENABLED = "true" }
if (-not $env:REACTOR_VM_ROOT) { $env:REACTOR_VM_ROOT = "$env:USERPROFILE\ReactorVMs" }
if (-not $env:REACTOR_VMRUN_BINARY) { $env:REACTOR_VMRUN_BINARY = "vmrun" }
if (-not $env:REACTOR_VMWARE_DISKMANAGER_BINARY) { $env:REACTOR_VMWARE_DISKMANAGER_BINARY = "vmware-vdiskmanager" }
if (-not $env:REACTOR_QEMU_IMG_BINARY) { $env:REACTOR_QEMU_IMG_BINARY = "qemu-img" }
if (-not $env:REACTOR_QEMU_SYSTEM_BINARY) { $env:REACTOR_QEMU_SYSTEM_BINARY = "qemu-system-x86_64" }

Write-Host "Reactor local VM host control"
Write-Host "VM execution enabled: $env:REACTOR_VM_EXECUTION_ENABLED"
Write-Host "VM root: $env:REACTOR_VM_ROOT"
Write-Host ""
Write-Host "Detected provider tools:"
Get-Command $env:REACTOR_VMRUN_BINARY -ErrorAction SilentlyContinue
Get-Command $env:REACTOR_VMWARE_DISKMANAGER_BINARY -ErrorAction SilentlyContinue
Get-Command $env:REACTOR_QEMU_IMG_BINARY -ErrorAction SilentlyContinue
Get-Command $env:REACTOR_QEMU_SYSTEM_BINARY -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "Starting backend on the physical host. Keep this PowerShell open while creating VMs."

if (Get-Command gradle -ErrorAction SilentlyContinue) {
  gradle --no-daemon :backend:bootRun
} else {
  Write-Error "Gradle is not installed. Install Gradle 8+ or run the backend from your IDE with the same environment variables."
}
