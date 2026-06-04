resource "aws_s3_bucket" "local_stack_s3_bucket" {
  bucket        = var.s3_bucket_name
  force_destroy = true
}

resource "aws_s3_bucket_versioning" "local_stack_s3_bucket_versioning" {
  bucket = aws_s3_bucket.local_stack_s3_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_sqs_queue" "local_stack_sqs_queue" {
  name = var.sqs_queue_name
}

resource "aws_sqs_queue_policy" "local_stack_sqs_queue_policy" {
  queue_url = aws_sqs_queue.local_stack_sqs_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowS3ToSendMessage"
        Effect = "Allow"
        Principal = {
          Service = "s3.amazonaws.com"
        }
        Action   = "sqs:SendMessage"
        Resource = aws_sqs_queue.local_stack_sqs_queue.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_s3_bucket.local_stack_s3_bucket.arn
          }
        }
      }
    ]
  })
}

resource "aws_s3_bucket_notification" "local_stack_s3_bucket_notification" {
  bucket = aws_s3_bucket.local_stack_s3_bucket.id

  queue {
    queue_arn = aws_sqs_queue.local_stack_sqs_queue.arn
    events    = ["s3:ObjectCreated:*"]
  }

  depends_on = [aws_sqs_queue_policy.local_stack_sqs_queue_policy]
}