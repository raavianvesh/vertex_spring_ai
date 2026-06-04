package org.example.configuration;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class DocumentAiConfig {

    @Bean(destroyMethod = "close")
    public DocumentProcessorServiceClient documentProcessorServiceClient() throws IOException {
        return DocumentProcessorServiceClient.create();
    }
}
