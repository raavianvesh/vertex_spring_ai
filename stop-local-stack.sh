#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="localstack/docker-compose.yaml"

if command -v docker.exe >/dev/null 2>&1; then
  DOCKER_BIN="docker.exe"
elif command -v docker >/dev/null 2>&1; then
  DOCKER_BIN="docker"
else
  echo "docker is not installed or not on PATH" >&2
  exit 1
fi

cd "$SCRIPT_DIR"

"$DOCKER_BIN" compose -f "$COMPOSE_FILE" down --remove-orphans --volumes

echo "LocalStack has been stopped."