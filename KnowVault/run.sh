#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if command -v mvn >/dev/null 2>&1; then exec mvn javafx:run; fi
exec ./mvnw javafx:run
