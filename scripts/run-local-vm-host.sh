#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

export REACTOR_VM_EXECUTION_ENABLED="${REACTOR_VM_EXECUTION_ENABLED:-true}"
export REACTOR_VM_ROOT="${REACTOR_VM_ROOT:-$HOME/ReactorVMs}"
export REACTOR_VMRUN_BINARY="${REACTOR_VMRUN_BINARY:-/Applications/VMware Fusion.app/Contents/Library/vmrun}"
export REACTOR_VMWARE_DISKMANAGER_BINARY="${REACTOR_VMWARE_DISKMANAGER_BINARY:-/Applications/VMware Fusion.app/Contents/Library/vmware-vdiskmanager}"
export REACTOR_QEMU_IMG_BINARY="${REACTOR_QEMU_IMG_BINARY:-qemu-img}"
export REACTOR_QEMU_SYSTEM_BINARY="${REACTOR_QEMU_SYSTEM_BINARY:-qemu-system-x86_64}"

echo "Reactor local VM host control"
echo "VM execution enabled: $REACTOR_VM_EXECUTION_ENABLED"
echo "VM root: $REACTOR_VM_ROOT"
echo
echo "Detected provider tools:"
command -v "$REACTOR_QEMU_IMG_BINARY" || true
command -v "$REACTOR_QEMU_SYSTEM_BINARY" || true
[[ -x "$REACTOR_VMRUN_BINARY" ]] && echo "$REACTOR_VMRUN_BINARY" || command -v vmrun || true
[[ -x "$REACTOR_VMWARE_DISKMANAGER_BINARY" ]] && echo "$REACTOR_VMWARE_DISKMANAGER_BINARY" || command -v vmware-vdiskmanager || true
echo
echo "Starting backend on the physical host. Keep this terminal open while creating VMs."

if command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon :backend:bootRun
else
  echo "Gradle is not installed. Install Gradle 8+ or run the backend from your IDE with the same environment variables." >&2
  exit 1
fi
