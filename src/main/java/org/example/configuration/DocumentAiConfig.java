package org.example.configuration;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class DocumentAiConfig {

    @Value("${gcp.document-ai.api-endpoint}")
    private String documentAiEndpoint;

    @Bean(destroyMethod = "close")
    public DocumentProcessorServiceClient documentProcessorServiceClient() throws IOException {
        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                .setEndpoint(documentAiEndpoint)
                .build();
        return DocumentProcessorServiceClient.create(settings);
    }
}
