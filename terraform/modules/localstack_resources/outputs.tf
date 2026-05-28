output "s3_bucket_name" {
  description = "Name of the S3 document store bucket"
  value       = aws_s3_bucket.local_stack_s3_bucket.bucket
}

output "s3_bucket_arn" {
  description = "ARN of the S3 document store bucket"
  value       = aws_s3_bucket.local_stack_s3_bucket.arn
}

output "dynamodb_table_name" {
  description = "Name of the DynamoDB document data table"
  value       = aws_dynamodb_table.local_stack_dynamodb_table.name
}

output "dynamodb_table_arn" {
  description = "ARN of the DynamoDB document data table"
  value       = aws_dynamodb_table.local_stack_dynamodb_table.arn
}
