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

echo "Deleting services..."
cd "$SCRIPT_DIR/terraform/modules/localstack_resources"

S3_BUCKET_NAME=$("$TERRAFORM_BIN" output -raw s3_bucket_name 2>/dev/null || echo "vertex-spring-ai-local-stack-s3-document-store")

echo "Emptying all object versions and delete markers from S3 bucket: $S3_BUCKET_NAME"
"$DOCKER_BIN" exec vertex-spring-ai-localstack sh -c '
set -eu
export AWS_PAGER=""
bucket="$1"

aws_local() {
  if command -v awslocal >/dev/null 2>&1; then
    awslocal "$@"
  else
    aws --endpoint-url=http://localhost:4566 "$@"
  fi
}

delete_all_from_list() {
  version_field="$1"

  while :; do
    delete_payload=$(aws_local s3api list-object-versions \
      --bucket "$bucket" \
      --max-items 1000 \
      --query "{Objects: ${version_field}[].{Key:Key,VersionId:VersionId}}" \
      --output json)

    if ! printf "%s" "$delete_payload" | grep -q "\"Key\""; then
      break
    fi

    aws_local s3api delete-objects \
      --bucket "$bucket" \
      --delete "$delete_payload" >/dev/null
  done
}

if aws_local s3api head-bucket --bucket "$bucket" >/dev/null 2>&1; then
  delete_all_from_list Versions
  delete_all_from_list DeleteMarkers
else
  echo "Bucket does not exist, skipping version cleanup: $bucket"
fi
' sh "$S3_BUCKET_NAME"

"$TERRAFORM_BIN" destroy -auto-approve

cd "$SCRIPT_DIR"

"$DOCKER_BIN" compose -f "$COMPOSE_FILE" down --remove-orphans --volumes
echo "LocalStack has been stopped."