#!/bin/sh
set -e
cd "$(dirname "$0")"
HTTP_PORT=46587
FCGI_PORT=46588
ROOT="$HOME/httpd-root"

mkdir -p "$ROOT/www" "$ROOT/fcgi-bin" "$ROOT/mutex-dir"
cp static/* "$ROOT/www/"
touch "$ROOT/fcgi-bin/server.jar"
sed -e "s|/home/studs/sXXXXXX|$HOME|g" -e "s|24000|$HTTP_PORT|g" -e "s|24001|$FCGI_PORT|g" httpd.conf > "$ROOT/httpd.conf"

(cd server && sh build.sh)

if [ -f "$ROOT/fcgi.pid" ]; then kill "$(cat "$ROOT/fcgi.pid")" 2>/dev/null || true; fi
nohup java -DFCGI_PORT=$FCGI_PORT -jar server/build/server.jar > "$ROOT/fcgi.log" 2>&1 &
echo $! > "$ROOT/fcgi.pid"

httpd -f "$ROOT/httpd.conf" -k restart
echo "Готово: http://helios.cs.ifmo.ru:$HTTP_PORT/"
