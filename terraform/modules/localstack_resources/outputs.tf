output "s3_bucket_name" {
  description = "Name of the S3 document store bucket"
  value       = aws_s3_bucket.local_stack_s3_bucket.bucket
}

output "s3_bucket_arn" {
  description = "ARN of the S3 document store bucket"
  value       = aws_s3_bucket.local_stack_s3_bucket.arn
}

output "sqs_queue_url" {
  description = "URL of the SQS queue for notifications"
  value       = aws_sqs_queue.local_stack_sqs_queue.url
}
