# AWS Native Mode (Submission Quick Notes)

This project now supports an AWS-first fraud pipeline:

- `Amazon S3` for document storage
- `Amazon Textract` for OCR extraction
- `Amazon Rekognition` for tamper signals
- `Amazon Rekognition Custom Labels` for forged/fake document classification
- `Amazon DynamoDB` for verification metadata + duplicate checks
- `AWS Lambda` optional risk boost scoring

## New Environment Variables

```bash
# Rekognition Custom Labels
AWS_REKOGNITION_CUSTOM_LABELS_ENABLED=true
AWS_REKOGNITION_CUSTOM_MODEL_ARN=arn:aws:rekognition:REGION:ACCOUNT:project/PROJECT/version/VERSION/...
AWS_REKOGNITION_CUSTOM_MIN_CONFIDENCE=70

# Optional Lambda risk scorer
AWS_LAMBDA_RISK_ENABLED=true
AWS_LAMBDA_RISK_FUNCTION_NAME=identity-risk-booster
AWS_LAMBDA_REGION=us-east-1
```

## Lambda Payload Contract

The backend sends this JSON to Lambda:

```json
{
  "fileHash": "sha256...",
  "identitySignatureHash": "sha256...",
  "tamperScore": 58,
  "duplicateDetected": true
}
```

Expected Lambda response format:

```json
{ "score": 0-20 }
```

## IAM Permissions Required

- `rekognition:DetectFaces`
- `rekognition:DetectLabels`
- `rekognition:DetectText`
- `rekognition:DetectModerationLabels`
- `rekognition:DetectCustomLabels`
- `textract:DetectDocumentText`
- `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, `s3:HeadBucket`
- `dynamodb:*` on verification tables (or least-privilege equivalent)
- `lambda:InvokeFunction` (if Lambda risk scoring enabled)

