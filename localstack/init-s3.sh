#!/bin/sh
set -eu

BUCKET_NAME="${BUCKET_NAME:-vertex-spring-ai-localstack-s3}"

awslocal s3api create-bucket --bucket "$BUCKET_NAME" >/dev/null 2>&1 || true
awslocal s3api put-bucket-versioning \
  --bucket "$BUCKET_NAME" \
  --versioning-configuration Status=Enabled

echo "LocalStack S3 bucket '$BUCKET_NAME' is ready with versioning enabled."

