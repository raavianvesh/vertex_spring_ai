package org.example.aws.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
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

    @SqsListener(value = "${aws.sqs.queue-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void listen(String receivedMessage, Acknowledgement acknowledgement) {
        try {
            acknowledgement.acknowledge();

            JsonNode jsonNode = objectMapper.readTree(receivedMessage);
            JsonNode recordsNode = jsonNode.path("Records");
            if (recordsNode.isMissingNode() || !recordsNode.isArray() || recordsNode.isEmpty()) {
                return;
            }
            postDocumentUploadService.handleS3Event(receivedMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse s3 event receivedMessage", e);
        }
    }
}
