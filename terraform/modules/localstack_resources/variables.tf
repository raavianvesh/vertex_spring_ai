variable "aws_access_key" {
  description = "AWS access key (use any non-empty value for LocalStack)"
  type        = string
  default     = "test"
}

variable "aws_secret_key" {
  description = "AWS secret key (use any non-empty value for LocalStack)"
  type        = string
  default     = "test"
  sensitive   = true
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "localstack_endpoint" {
  description = "LocalStack endpoint URL"
  type        = string
  default     = "http://localhost:4566"
}

variable "s3_bucket_name" {
  description = "Name of the S3 bucket for document storage"
  type        = string
  default     = "vertex-spring-ai-local-stack-s3-document-store"
}

variable "sqs_queue_name" {
  description = "Name of the SQS queue for notifications"
  type        = string
  default     = "vertex-spring-ai-local-stack-sqs-queue"
}