#!/bin/sh
ROOT="$HOME/httpd-root"
if [ -f "$ROOT/fcgi.pid" ]; then kill "$(cat "$ROOT/fcgi.pid")" 2>/dev/null; rm -f "$ROOT/fcgi.pid"; fi
httpd -f "$ROOT/httpd.conf" -k stop
echo "Остановлено"
