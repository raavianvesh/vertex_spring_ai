package org.example.tranformation;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessorName;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.Document.Page.Paragraph;
import com.google.protobuf.ByteString;
import org.example.utils.PdfService.SpringDocumentConversion;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TransformationService {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.location}")
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

    public List<Document> chunkWithGcpDocumentAi(SpringDocumentConversion springDocumentConversion) {
        List<Document> springAiChunks = new ArrayList<>();
        try {
            String name = ProcessorName.of(projectId, location, processorId).toString();
            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(ByteString.fromHex(Objects.requireNonNull(springDocumentConversion.springDocument().getText())))
                    .setMimeType(springDocumentConversion.springDocument().getMetadata().get("mimeType").toString())
                    .build();
            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(name)
                    .setRawDocument(rawDocument)
                    .build();
            // 3. Request remote structural processing from GCP
            ProcessResponse response = documentProcessorServiceClient.processDocument(request);
            com.google.cloud.documentai.v1.Document gcpDoc = response.getDocument();
            // 4. Iterate through GCP's structural chunks/paragraphs
            for (Paragraph paragraph : gcpDoc.getPages(0).getParagraphsList()) {
                // Extract segment text using text anchor offsets
                int startIndex = (int) paragraph.getLayout().getTextAnchor().getTextSegments(0).getStartIndex();
                int endIndex = (int) paragraph.getLayout().getTextAnchor().getTextSegments(0).getEndIndex();
                String chunkText = gcpDoc.getText().substring(startIndex, endIndex);

                // 5. Convert to Spring AI Document object
                Document springAiDoc = new Document(chunkText, Map.of("source", "gcp-document-ai-layout"));
                springAiChunks.add(springAiDoc);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to process document with GCP Document AI", e);
        }
        return springAiChunks;
    }
}
