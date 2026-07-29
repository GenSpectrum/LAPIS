#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
STATE_DIR="${LAPIS_PREVIEW_STATE_DIR:-/tmp/lapis-views-preview-$(id -u)}"
PID_FILE="$STATE_DIR/pid"
PORT_FILE="$STATE_DIR/port"
LOG_FILE="$STATE_DIR/lapis.log"

PORT="${LAPIS_PREVIEW_PORT:-8090}"
SILO_URL="${LAPIS_PREVIEW_SILO_URL:-https://gs-staging-1.int.genspectrum.org/open/v2/silo}"
VIEWS_CONFIG="$SCRIPT_DIR/preview/views.yaml"

usage() {
    echo "Usage: $0 {start|restart|recreate|stop|status|logs}"
}

read_pid() {
    if [[ -f "$PID_FILE" ]]; then
        tr -d '[:space:]' < "$PID_FILE"
    fi
}

process_is_preview() {
    local pid="$1"
    local process_cwd
    local command_line

    [[ "$pid" =~ ^[0-9]+$ ]] || return 1
    [[ -d "/proc/$pid" ]] || return 1

    process_cwd="$(readlink -f "/proc/$pid/cwd" 2>/dev/null || true)"
    command_line="$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true)"

    [[ "$process_cwd" == "$SCRIPT_DIR" ]] &&
        [[ "$command_line" == *bootRun* ]] &&
        { [[ "$command_line" == *gradle-wrapper.jar* ]] || [[ "$command_line" == *GradleWrapperMain* ]]; }
}

http_ready() {
    local port="$1"
    python3 - "$port" <<'PY'
import sys
from urllib.request import urlopen

try:
    with urlopen(f"http://127.0.0.1:{sys.argv[1]}/actuator/health", timeout=2) as response:
        raise SystemExit(0 if response.status == 200 else 1)
except Exception:
    raise SystemExit(1)
PY
}

port_is_available() {
    python3 - "$PORT" <<'PY'
import socket
import sys

with socket.socket() as sock:
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        sock.bind(("127.0.0.1", int(sys.argv[1])))
    except OSError:
        raise SystemExit(1)
PY
}

wait_for_port_release() {
    for _ in {1..30}; do
        if port_is_available; then
            return
        fi
        sleep 1
    done

    echo "Port $PORT is still in use after stopping the preview." >&2
    exit 1
}

start_preview() {
    local pid

    mkdir -p "$STATE_DIR"
    pid="$(read_pid)"
    if process_is_preview "$pid"; then
        echo "LAPIS preview is already running at http://127.0.0.1:$PORT (PID $pid)."
        return
    fi

    if ! port_is_available; then
        echo "Port $PORT is already in use by another process." >&2
        exit 1
    fi

    rm -f "$PID_FILE" "$PORT_FILE"
    : > "$LOG_FILE"

    (
        cd "$SCRIPT_DIR"
        exec setsid ./gradlew --no-daemon --console=plain bootRun \
            --args="--server.address=127.0.0.1 --server.port=$PORT --silo.url=$SILO_URL --lapis.viewsConfig.path=$VIEWS_CONFIG"
    ) >> "$LOG_FILE" 2>&1 < /dev/null &
    pid=$!
    printf '%s\n' "$pid" > "$PID_FILE"
    printf '%s\n' "$PORT" > "$PORT_FILE"

    echo "Starting LAPIS preview against $SILO_URL (PID $pid)..."
    for attempt in {1..120}; do
        if ! kill -0 "$pid" 2>/dev/null || { [[ "$attempt" -gt 5 ]] && ! process_is_preview "$pid"; }; then
            echo "LAPIS preview exited during startup. Recent log output:" >&2
            tail -n 80 "$LOG_FILE" >&2
            rm -f "$PID_FILE" "$PORT_FILE"
            exit 1
        fi
        if http_ready "$PORT"; then
            echo "LAPIS preview is ready at http://127.0.0.1:$PORT/."
            echo "Logs: $LOG_FILE"
            return
        fi
        sleep 1
    done

    echo "LAPIS preview did not become healthy within 120 seconds. Recent log output:" >&2
    tail -n 80 "$LOG_FILE" >&2
    stop_preview
    exit 1
}

stop_preview() {
    local pid

    pid="$(read_pid)"
    if ! process_is_preview "$pid"; then
        echo "LAPIS preview is not running."
        rm -f "$PID_FILE" "$PORT_FILE"
        return
    fi

    echo "Stopping LAPIS preview (PID $pid)..."
    kill -TERM -- "-$pid"
    for _ in {1..20}; do
        if ! process_is_preview "$pid"; then
            wait_for_port_release
            rm -f "$PID_FILE" "$PORT_FILE"
            echo "LAPIS preview stopped."
            return
        fi
        sleep 0.5
    done

    kill -KILL -- "-$pid" 2>/dev/null || true
    wait_for_port_release
    rm -f "$PID_FILE" "$PORT_FILE"
    echo "LAPIS preview stopped."
}

show_status() {
    local pid
    local running_port="$PORT"

    pid="$(read_pid)"
    if [[ -f "$PORT_FILE" ]]; then
        running_port="$(tr -d '[:space:]' < "$PORT_FILE")"
    fi

    if process_is_preview "$pid"; then
        if http_ready "$running_port"; then
            echo "LAPIS preview is healthy at http://127.0.0.1:$running_port (PID $pid)."
        else
            echo "LAPIS preview process is running but is not healthy yet (PID $pid)."
            return 1
        fi
    else
        echo "LAPIS preview is not running."
        return 1
    fi
}

show_logs() {
    if [[ ! -f "$LOG_FILE" ]]; then
        echo "No preview log exists at $LOG_FILE." >&2
        exit 1
    fi

    if [[ "${2:-}" == "--follow" ]]; then
        tail -n 200 -f "$LOG_FILE"
    else
        tail -n 200 "$LOG_FILE"
    fi
}

case "${1:-}" in
    start)
        start_preview
        ;;
    restart|recreate)
        stop_preview
        start_preview
        ;;
    stop)
        stop_preview
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$@"
        ;;
    *)
        usage >&2
        exit 2
        ;;
esac
