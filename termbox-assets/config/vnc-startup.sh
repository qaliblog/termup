#!/bin/bash
# vnc-startup.sh - Start VNC server with XFCE desktop in Ubuntu proot
# This script is run inside the Ubuntu proot environment

set -euo pipefail

# Configuration
VNC_DISPLAY=":1"
VNC_PORT="5901"
VNC_GEOMETRY="1920x1080"
VNC_DEPTH="24"

# Get the proot directory
PROOT_DIR="${TERMUX_APP_DATA_DIR:-/data/data/com.qali.termup}/files/proot-distro/installed-rootfs/ubuntu"
VNC_DIR="${PROOT_DIR}/root/.vnc"

echo "Starting VNC server with XFCE desktop..."

# Create VNC directory if it doesn't exist
mkdir -p "${VNC_DIR}"

# Create xstartup if it doesn't exist
if [ ! -f "${VNC_DIR}/xstartup" ]; then
    cat > "${VNC_DIR}/xstartup" << 'XSTARTEOF'
#!/bin/bash
unset SESSION_MANAGER
unset DBUS_SESSION_BUS_ADDRESS
export XDG_RUNTIME_DIR="/run/user/$(id -u)"
export XDG_SESSION_TYPE="x11"
export XDG_CURRENT_DESKTOP="XFCE"
export XDG_SESSION_DESKTOP="xfce"
export XDG_SESSION_CLASS="user"

# Start dbus if not running
if [ -z "$DBUS_SESSION_BUS_ADDRESS" ]; then
    eval $(dbus-launch --sh-syntax --exit-with-session)
fi

# Start XFCE session
exec startxfce4
XSTARTEOF
    chmod +x "${VNC_DIR}/xstartup"
fi

# Create VNC config
if [ ! -f "${VNC_DIR}/config" ]; then
    cat > "${VNC_DIR}/config" << 'VNCEOF'
geometry=1920x1080
depth=24
localhost=no
SecurityTypes=None
VNCEOF
fi

# Start VNC server
echo "Starting VNC server on display ${VNC_DISPLAY}..."
cd "${PROOT_DIR}"
proot-distro login ubuntu --user root -- bash -c "
    export HOME=/root
    export USER=root
    export DISPLAY=${VNC_DISPLAY}
    vncserver ${VNC_DISPLAY} -geometry ${VNC_GEOMETRY} -depth ${VNC_DEPTH} -localhost no -SecurityTypes None 2>&1
    echo 'VNC server started on port 5901'
"

# Wait for VNC server to be ready
sleep 3

# Check if VNC server is running
if netstat -tlnp 2>/dev/null | grep -q ":5901"; then
    echo "VNC server is running on port 5901"
    exit 0
else
    echo "Failed to start VNC server"
    exit 1
fi