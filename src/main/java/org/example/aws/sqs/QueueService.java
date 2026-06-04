package org.example.aws.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.example.service.PostDocumentUploadService;
import org.springframework.stereotype.Component;

@Component
public class QueueService{

    private final ObjectMapper objectMapper;
    private final PostDocumentUploadService postDocumentUploadService;
    public QueueService(PostDocumentUploadService postDocumentUploadService, ObjectMapper objectMapper) {
        this.postDocumentUploadService = postDocumentUploadService;
        this.objectMapper = objectMapper;
    }

    @SqsListener("${aws.sqs.queue-name}")
    public void listen(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode recordsNode = jsonNode.path("Records");
            if (recordsNode.isMissingNode() || !recordsNode.isArray() || recordsNode.isEmpty()) {
                return;
            }
            postDocumentUploadService.handleS3Event(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse s3 event message", e);
        }
    }
}
