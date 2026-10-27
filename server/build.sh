#!/bin/sh
# Сборка: lib/fastcgi-lib.jar -> build/server.jar
set -e
cd "$(dirname "$0")"
rm -rf build
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -cp lib/fastcgi-lib.jar -d build/classes src/lab2/*.java
printf 'Main-Class: lab2.Main\nClass-Path: fastcgi-lib.jar\n' > build/manifest.txt
cp lib/fastcgi-lib.jar build/
jar cfm build/server.jar build/manifest.txt -C build/classes .
echo "OK: build/server.jar"
