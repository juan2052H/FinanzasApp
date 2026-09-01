#!/usr/bin/env sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
PROPS="$WRAPPER_DIR/maven-wrapper.properties"
VERSION="$(sed -n 's/^distributionVersion=//p' "$PROPS" | tr -d '\r')"
URL="$(sed -n 's/^distributionUrl=//p' "$PROPS" | tr -d '\r')"
EXPECTED_SHA512="$(sed -n 's/^distributionSha512=//p' "$PROPS" | tr -d '\r' | tr 'A-F' 'a-f')"
DIST_DIR="$WRAPPER_DIR/dists"
ZIP_PATH="$DIST_DIR/apache-maven-$VERSION-bin.zip"
MAVEN_HOME="$DIST_DIR/apache-maven-$VERSION"

mkdir -p "$DIST_DIR"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  if [ ! -f "$ZIP_PATH" ]; then
    echo "Downloading Apache Maven $VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$URL" -O "$ZIP_PATH"
    else
      echo "curl or wget is required to download Maven." >&2
      exit 1
    fi
  fi

  if command -v sha512sum >/dev/null 2>&1; then
    ACTUAL_SHA512="$(sha512sum "$ZIP_PATH" | awk '{print tolower($1)}')"
  elif command -v shasum >/dev/null 2>&1; then
    ACTUAL_SHA512="$(shasum -a 512 "$ZIP_PATH" | awk '{print tolower($1)}')"
  else
    echo "sha512sum or shasum is required to verify Maven." >&2
    exit 1
  fi

  if [ "$ACTUAL_SHA512" != "$EXPECTED_SHA512" ]; then
    rm -f "$ZIP_PATH"
    echo "Maven distribution checksum mismatch." >&2
    exit 1
  fi

  EXTRACT_TMP="$DIST_DIR/extract-$VERSION"
  rm -rf "$EXTRACT_TMP"
  mkdir -p "$EXTRACT_TMP"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP_PATH" -d "$EXTRACT_TMP"
  else
    (cd "$EXTRACT_TMP" && jar xf "$ZIP_PATH")
  fi
  rm -rf "$MAVEN_HOME"
  mv "$EXTRACT_TMP/apache-maven-$VERSION" "$MAVEN_HOME"
  rm -rf "$EXTRACT_TMP"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
