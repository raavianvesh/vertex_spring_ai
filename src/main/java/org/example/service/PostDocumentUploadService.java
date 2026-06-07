package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tranformation.TransformationService;
import org.example.utils.PdfService.SpringDocumentConversion;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PostDocumentUploadService {
    private final ObjectMapper objectMapper;
    private final S3Client s3Client;
    private final ParseAndEnrichDocumentService parseAndEnrichDocumentService;
    private final TransformationService transformationService;
    private final VectorStore vectorStore;


    public PostDocumentUploadService(ObjectMapper objectMapper, S3Client s3Client, ParseAndEnrichDocumentService parseAndEnrichDocumentService, TransformationService transformationService, VectorStore vectorStore) {
        this.objectMapper = objectMapper;
        this.s3Client = s3Client;
        this.parseAndEnrichDocumentService = parseAndEnrichDocumentService;
        this.transformationService = transformationService;
        this.vectorStore = vectorStore;
    }

    public void handleS3Event(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        JsonNode bucketNode = root.path("Records").path(0).path("s3").path("bucket");
        JsonNode objectNode = root.path("Records").path(0).path("s3").path("object");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketNode.path("name").asText())
                .key(decode(objectNode, "key"))
                .versionId(objectNode.path("versionId").asText())
                .build();
        try (ResponseInputStream<GetObjectResponse> s3FileStream = s3Client.getObject(getObjectRequest)) {
            byte[] bytes = s3FileStream.readAllBytes();
            Resource resource = new ByteArrayResource(bytes);
            // Scan the ocr document, get text , parse and enrich
            SpringDocumentConversion springDocumentConversion = parseAndEnrichDocumentService.processDocument(resource, getObjectRequest.key(), getObjectRequest.versionId(), getObjectRequest.bucket());
            // chunk the text
            List<Document> springAiChunks = transformationService.chunkTextUsingSpringAi(springDocumentConversion);
            List<Document> documentAiChunks = transformationService.chunkWithGcpDocumentAi(springDocumentConversion);
            // combine chunks
            List<Document> vectorStoreDocuments = transformationService.combineTextChunksWithDocumentAiMetadata(
                    springDocumentConversion,
                    springAiChunks,
                    documentAiChunks);
            // embed Spring AI text chunks and store them with original document + Document AI metadata
            vectorStore.write(vectorStoreDocuments);
        }
    }

    public String decode(JsonNode node, String key) {
        return URLDecoder.decode(
                node.path(key).asText(),
                StandardCharsets.UTF_8);
    }

}
