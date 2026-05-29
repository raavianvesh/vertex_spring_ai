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

if command -v terraform.exe >/dev/null 2>&1; then
  TERRAFORM_BIN="terraform.exe"
elif command -v terraform >/dev/null 2>&1; then
  TERRAFORM_BIN="terraform"
else
  echo "terraform is not installed or not on PATH" >&2
  exit 1
fi

if ! "$DOCKER_BIN" info >/dev/null 2>&1; then
  echo "docker daemon is not running. Start Docker Desktop and try again." >&2
  exit 1
fi

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "Compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

cd "$SCRIPT_DIR"

echo "Starting LocalStack and waiting for it to be healthy..."
"$DOCKER_BIN" compose -f "$COMPOSE_FILE" up -d --wait
echo "LocalStack is ready."

echo "Running Terraform..."
cd "$SCRIPT_DIR/terraform/modules/localstack_resources"
"$TERRAFORM_BIN" init -input=false
"$TERRAFORM_BIN" apply -auto-approve

echo "LocalStack is running."
echo "Endpoint: http://localhost:4566"
echo "S3 bucket: vertex-spring-ai-local-stack-s3-document-store"
echo "DynamoDB table: vertex-spring-ai-local-stack-dynamodb-document-data"

