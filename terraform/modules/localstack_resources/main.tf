terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.36.0"
    }
  }
}

provider "aws" {
  region                      = var.region
  access_key                  = var.aws_access_key
  secret_key                  = var.aws_secret_key
  skip_credentials_validation = true
  skip_requesting_account_id  = true
  s3_use_path_style           = true

  endpoints {
    s3       = var.localstack_endpoint
    dynamodb = var.localstack_endpoint
  }
}

resource "aws_s3_bucket" "local_stack_s3_bucket" {
  bucket = var.s3_bucket_name
}

resource "aws_s3_bucket_versioning" "local_stack_s3_bucket_versioning" {
  bucket = aws_s3_bucket.local_stack_s3_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_dynamodb_table" "local_stack_dynamodb_table" {
  name         = var.dynamodb_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "file_number"
  range_key    = "create_at"

  attribute {
    name = "file_number"
    type = "S"
  }

  attribute {
    name = "create_at"
    type = "N"
  }

  attribute {
    name = "update_at"
    type = "N"
  }

  attribute {
    name = "file_name"
    type = "S"
  }

  global_secondary_index {
    name            = "file-name-index"
    projection_type = "ALL"
    key_schema {
      attribute_name = "file_name"
      key_type       = "HASH"
    }
  }

  global_secondary_index {
    name            = "update-at-index"
    projection_type = "ALL"
    key_schema {
      attribute_name = "update_at"
      key_type       = "HASH"
    }
  }
}
