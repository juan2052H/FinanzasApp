#!/bin/sh
# Genera capturas reales de las pantallas en docs/capturas/ usando una pantalla virtual (Xvfb).
# Requiere el ambiente arriba con datos recien sembrados:
#   docker compose down -v && docker compose up -d --build
#   docker run --rm --network finanzasapp_finanzas-internal -v "$PWD":/src -w /src \
#       eclipse-temurin:25-jdk sh scripts/capturas/capturar.sh
# Capturas del manual de instalacion: agregar -e CAPTURAS_CLASE=CapturarInstalacion
# Fuentes de Windows (opcional, para que se vea igual que en Windows): -v C:/Windows/Fonts:/winfonts:ro
set -e
apt-get update -qq >/dev/null
apt-get install -y -qq --no-install-recommends xvfb libxext6 libxrender1 libxtst6 libxi6 libfontconfig1 libfreetype6 fonts-dejavu fonts-liberation >/dev/null

export FINANZAS_API_ENABLED=true
export FINANZAS_API_BASE_URL="${FINANZAS_API_BASE_URL:-http://backend:8080}"
export FINANZAS_DATA_DIR=/tmp/finanzas-capturas
if [ -d /winfonts ]; then
    mkdir -p /usr/share/fonts/truetype/windows
    cp /winfonts/segoeui*.ttf /winfonts/seguiemj.ttf /winfonts/seguisym.ttf /usr/share/fonts/truetype/windows/ 2>/dev/null || true
    fc-cache -f >/dev/null 2>&1 || true
fi
CLASE="${CAPTURAS_CLASE:-CapturarPantallas}"

# Usa el jar del cliente ya compilado (incluye FlatLaf): ./mvnw -f pom.xml -DskipTests package
JAR=target/FinanzasApp-shaded.jar
[ -f "$JAR" ] || { echo "Falta $JAR: ejecute ./mvnw -f pom.xml -DskipTests package"; exit 1; }
rm -rf /tmp/capturas-build && mkdir -p /tmp/capturas-build
javac --release 25 -encoding UTF-8 -cp "$JAR" -d /tmp/capturas-build "scripts/capturas/$CLASE.java"

Xvfb :99 -screen 0 1280x800x24 >/dev/null 2>&1 &
export DISPLAY=:99
sleep 2
java -Dawt.useSystemAAFontSettings=on -cp "$JAR:/tmp/capturas-build" "$CLASE" "${CAPTURAS_SALIDA:-docs/capturas}" "${CAPTURAS_CORREO:-ana.duena@finanzasapp.test}"
