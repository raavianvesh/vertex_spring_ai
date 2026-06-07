package org.example.tranformation;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessorName;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import org.example.utils.PdfService.SpringDocumentConversion;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TransformationService {

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${gcp.document-ai.location}")
    private String location;

    @Value("${gcp.processor-id}")
    private String processorId;
    private final DocumentProcessorServiceClient documentProcessorServiceClient;

    public TransformationService(DocumentProcessorServiceClient documentProcessorServiceClient) {
        this.documentProcessorServiceClient = documentProcessorServiceClient;
    }

    public List<Document> chunkTextUsingSpringAi(SpringDocumentConversion springDocumentConversion) {
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkSizeChars(500)
                .withKeepSeparator(false)
                .withMaxNumChunks(-1)
                .withMinChunkLengthToEmbed(100)
                .build();
        return textSplitter.apply(Collections.singletonList(springDocumentConversion.springDocument()));
    }

    public List<Document> combineTextChunksWithDocumentAiMetadata(SpringDocumentConversion springDocumentConversion,
                                                                  List<Document> springAiChunks,
                                                                  List<Document> documentAiChunks) {
        Map<String, Object> originalDocumentMetadata = new LinkedHashMap<>(springDocumentConversion.springDocument().getMetadata());
        List<Document> combinedDocuments = new ArrayList<>();

        if (springAiChunks.isEmpty()) {
            for (int documentAiIndex = 0; documentAiIndex < documentAiChunks.size(); documentAiIndex++) {
                Document documentAiChunk = documentAiChunks.get(documentAiIndex);
                String documentAiText = Objects.toString(documentAiChunk.getText(), "");
                Map<String, Object> combinedMetadata = new LinkedHashMap<>(originalDocumentMetadata);
                combinedMetadata.put("chunk_index", documentAiIndex);
                combinedMetadata.put("chunk_count", documentAiChunks.size());
                combinedMetadata.put("chunk_text_length", documentAiText.length());
                combinedMetadata.put("chunking_source", "gcp-document-ai-direct-fallback");
                addDocumentAiMetadata(combinedMetadata, List.of(documentAiChunk));

                combinedDocuments.add(new Document(documentAiChunk.getId(), documentAiText, combinedMetadata));
            }
            return combinedDocuments;
        }

        for (int chunkIndex = 0; chunkIndex < springAiChunks.size(); chunkIndex++) {
            Document springAiChunk = springAiChunks.get(chunkIndex);
            String chunkText = Objects.toString(springAiChunk.getText(), "");
            Map<String, Object> combinedMetadata = new LinkedHashMap<>(originalDocumentMetadata);
            combinedMetadata.putAll(springAiChunk.getMetadata());
            combinedMetadata.put("chunk_index", chunkIndex);
            combinedMetadata.put("chunk_count", springAiChunks.size());
            combinedMetadata.put("chunk_text_length", chunkText.length());
            combinedMetadata.put("chunking_source", "spring-ai-token-text-splitter");

            addDocumentAiMetadata(combinedMetadata, documentAiChunks);

            combinedDocuments.add(new Document(springAiChunk.getId(), chunkText, combinedMetadata));
        }

        return combinedDocuments;
    }

    public List<Document> chunkWithGcpDocumentAi(SpringDocumentConversion springDocumentConversion) {
        List<Document> springAiChunks = new ArrayList<>();
        try {
            String name = ProcessorName.of(projectId, location, processorId).toString();
            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(ByteString.copyFrom(Objects.requireNonNull(springDocumentConversion.verificationPdfBytes())))
                    .setMimeType(springDocumentConversion.springDocument().getMetadata().get("mimeType").toString())
                    .build();
            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(name)
                    .setRawDocument(rawDocument)
                    .build();
            // 3. Request remote structural processing from GCP https://us-documentai.googleapis.com/v1/projects/364520431840/locations/us/processors/856dbce4058074c6:process
            ProcessResponse response = documentProcessorServiceClient.processDocument(request);
            com.google.cloud.documentai.v1.Document gcpDoc = response.getDocument();
            // 4. Convert the GCP Document AI protobuf directly into one Spring AI Document.
            String documentText = gcpDoc.getText();
            String documentJson = JsonFormat.printer()
                    .preservingProtoFieldNames()
                    .omittingInsignificantWhitespace()
                    .print(gcpDoc);

            Map<String, Object> metadata = getStringObjectMap(documentText, documentJson, gcpDoc);

            springAiChunks.add(new Document(documentText.isBlank() ? documentJson : documentText, metadata));
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to process document with GCP Document AI", e);
        }
        return springAiChunks;
    }

    private void addDocumentAiMetadata(Map<String, Object> metadata, List<Document> documentAiChunks) {
        metadata.put("document_ai_document_count", documentAiChunks.size());

        if (documentAiChunks.size() == 1) {
            addPrefixedDocumentMetadata(metadata, "document_ai", documentAiChunks.getFirst());
            return;
        }

        for (int documentAiIndex = 0; documentAiIndex < documentAiChunks.size(); documentAiIndex++) {
            addPrefixedDocumentMetadata(metadata, "document_ai_" + documentAiIndex, documentAiChunks.get(documentAiIndex));
        }
    }

    private void addPrefixedDocumentMetadata(Map<String, Object> targetMetadata, String prefix, Document sourceDocument) {
        String documentText = Objects.toString(sourceDocument.getText(), "");
        targetMetadata.put(prefix + "_text", documentText);
        targetMetadata.put(prefix + "_text_length", documentText.length());
        sourceDocument.getMetadata().forEach((key, value) -> targetMetadata.put(prefix + "_" + key, value));
    }

    private static @NonNull Map<String, Object> getStringObjectMap(String documentText, String documentJson, com.google.cloud.documentai.v1.Document gcpDoc) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "gcp-document-ai-direct");
        metadata.put("document_text", documentText);
        metadata.put("document_text_length", documentText.length());
        metadata.put("gcp_document_ai_json", documentJson);
        metadata.put("gcp_document_ai_page_count", gcpDoc.getPagesCount());
        metadata.put("gcp_document_ai_has_chunked_document", gcpDoc.hasChunkedDocument());
        metadata.put("gcp_document_ai_chunk_count", gcpDoc.hasChunkedDocument() ? gcpDoc.getChunkedDocument().getChunksCount() : 0);
        return metadata;
    }
}
