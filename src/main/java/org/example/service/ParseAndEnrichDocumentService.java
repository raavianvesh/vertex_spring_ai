package org.example.service;

import com.google.cloud.spring.vision.CloudVisionTemplate;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.Feature;
import org.example.utils.PdfService;
import org.example.utils.PdfService.SpringDocumentConversion;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class ParseAndEnrichDocumentService {
    private final CloudVisionTemplate cloudVisionTemplate;
    private final PdfService pdfService;

    public ParseAndEnrichDocumentService(CloudVisionTemplate cloudVisionTemplate, PdfService pdfService) {
        this.cloudVisionTemplate = cloudVisionTemplate;
        this.pdfService = pdfService;
    }

    /**
     * This method processes the document using Google Cloud Vision API to extract text and other relevant information.
     * It takes a Resource representing the document stream, the document name, and its version ID as parameters.
     * The method returns a Spring AI document conversion result containing a lightweight Spring AI document
     * for transformation/vector storage and a generated PDF artifact for development verification.
     * @param documentStream the input stream of the document to be processed
     * @param documentName the name of the document being processed
     * @param versionId the version ID of the document being processed
     * @return a conversion result containing the Spring AI document and verification PDF artifact details
     */
    public SpringDocumentConversion processDocument(Resource documentStream, String documentName, String versionId, String bucketName) {
        AnnotateFileResponse annotateFileResponse = cloudVisionTemplate.analyzeFile(documentStream,
                MediaType.APPLICATION_PDF_VALUE,
                Feature.Type.DOCUMENT_TEXT_DETECTION);
        return pdfService.convertToSpringDocument(annotateFileResponse, documentName, versionId, bucketName, false);
    }
}
