#!/bin/sh
# Genera capturas reales de las pantallas en docs/capturas/ usando una pantalla virtual (Xvfb).
# Requiere el ambiente arriba con datos recien sembrados:
#   docker compose down -v && docker compose up -d --build
#   docker run --rm --network finanzasapp_finanzas-internal -v "$PWD":/src -w /src \
#       eclipse-temurin:25-jdk sh scripts/capturas/capturar.sh
set -e
apt-get update -qq >/dev/null
apt-get install -y -qq --no-install-recommends xvfb libxext6 libxrender1 libxtst6 libxi6 libfontconfig1 libfreetype6 fonts-dejavu fonts-liberation >/dev/null

export FINANZAS_API_ENABLED=true
export FINANZAS_API_BASE_URL="${FINANZAS_API_BASE_URL:-http://backend:8080}"
export FINANZAS_DATA_DIR=/tmp/finanzas-capturas

rm -rf /tmp/capturas-build && mkdir -p /tmp/capturas-build
find src/main/java -name "*.java" > /tmp/capturas-build/sources.txt
javac --release 25 -encoding UTF-8 -d /tmp/capturas-build @/tmp/capturas-build/sources.txt
cp -r src/main/resources/. /tmp/capturas-build/ 2>/dev/null || true
javac --release 25 -encoding UTF-8 -cp /tmp/capturas-build -d /tmp/capturas-build scripts/capturas/CapturarPantallas.java

Xvfb :99 -screen 0 1280x800x24 >/dev/null 2>&1 &
export DISPLAY=:99
sleep 2
java -Dawt.useSystemAAFontSettings=on -cp /tmp/capturas-build CapturarPantallas docs/capturas
