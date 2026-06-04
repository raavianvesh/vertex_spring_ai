package org.example.utils;


import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Word;
import com.google.cloud.vision.v1.Symbol;
import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfService {

    public record SpringDocumentConversion(
            org.springframework.ai.document.Document springDocument,
            byte[] verificationPdfBytes,
            Path verificationPdfPath
    ) {
    }

    public SpringDocumentConversion convertToSpringDocument(AnnotateFileResponse annotateFileResponse,
                                                            String name,
                                                            String versionId,
                                                            String bucketName,
                                                            boolean writePdf) {
        if (annotateFileResponse == null) {
            throw new IllegalArgumentException("annotateFileResponse cannot be null");
        }

        if (annotateFileResponse.getResponsesCount() == 0) {
            throw new IllegalArgumentException("Vision API response contains no page responses");
        }

        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

            int renderedPageCount = 0;
            StringBuilder fullText = new StringBuilder();

            for (AnnotateImageResponse pageResponse : annotateFileResponse.getResponsesList()) {
                if (pageResponse.hasError()) {
                    continue;
                }

                if (pageResponse.hasFullTextAnnotation()) {
                    fullText.append(pageResponse.getFullTextAnnotation().getText()).append("\n");
                }

                var annotation = pageResponse.getFullTextAnnotation();

                if (annotation.getPagesCount() == 0) {
                    continue;
                }

                for (Page visionPage : annotation.getPagesList()) {
                    if (renderedPageCount == 0) {
                        document.open();
                    }
                    document.newPage();
                    document.add(new Paragraph(" "));
                    renderedPageCount++;
                    PdfContentByte cb = writer.getDirectContent();

                    int visionWidth = visionPage.getWidth() > 0 ? visionPage.getWidth() : 800;
                    int visionHeight = visionPage.getHeight() > 0 ? visionPage.getHeight() : 1000;

                    float scaleX = PageSize.LETTER.getWidth() / (float) visionWidth;
                    float scaleY = PageSize.LETTER.getHeight() / (float) visionHeight;

                    for (Block block : visionPage.getBlocksList()) {
                        if (block.hasBoundingBox()) {
                            var vertices = block.getBoundingBox().getVerticesList();
                            var normalizedVertices = block.getBoundingBox().getNormalizedVerticesList();

                            if (vertices.size() >= 4) {
                                float minX = vertices.get(0).getX() * scaleX;
                                float maxY = PageSize.LETTER.getHeight() - (vertices.get(0).getY() * scaleY);
                                float maxX = vertices.get(2).getX() * scaleX;
                                float minY = PageSize.LETTER.getHeight() - (vertices.get(2).getY() * scaleY);

                                cb.setLineWidth(0.5f);
                                cb.setColorStroke(new Color(0, 102, 204, 80));
                                cb.rectangle(minX, minY, maxX - minX, maxY - minY);
                                cb.stroke();
                            } else if (normalizedVertices.size() >= 4) {
                                float minX = normalizedVertices.get(0).getX() * PageSize.LETTER.getWidth();
                                float maxY = PageSize.LETTER.getHeight() - (normalizedVertices.get(0).getY() * PageSize.LETTER.getHeight());
                                float maxX = normalizedVertices.get(2).getX() * PageSize.LETTER.getWidth();
                                float minY = PageSize.LETTER.getHeight() - (normalizedVertices.get(2).getY() * PageSize.LETTER.getHeight());

                                cb.setLineWidth(0.5f);
                                cb.setColorStroke(new Color(0, 102, 204, 80));
                                cb.rectangle(minX, minY, maxX - minX, maxY - minY);
                                cb.stroke();
                            }
                        }

                        for (com.google.cloud.vision.v1.Paragraph paragraph : block.getParagraphsList()) {
                            for (Word word : paragraph.getWordsList()) {
                                StringBuilder wordText = new StringBuilder();

                                for (Symbol symbol : word.getSymbolsList()) {
                                    wordText.append(symbol.getText());
                                }

                                if (wordText.isEmpty()) {
                                    continue;
                                }

                                if (word.hasBoundingBox()) {
                                    var vertices = word.getBoundingBox().getVerticesList();
                                    var normalizedVertices = word.getBoundingBox().getNormalizedVerticesList();

                                    float absoluteX = 0;
                                    float absoluteY = 0;
                                    boolean hasCoords = false;

                                    if (!vertices.isEmpty()) {
                                        var wordStartPos = vertices.get(0);
                                        absoluteX = wordStartPos.getX() * scaleX;
                                        absoluteY = PageSize.LETTER.getHeight() - (wordStartPos.getY() * scaleY);
                                        hasCoords = true;
                                    } else if (!normalizedVertices.isEmpty()) {
                                        var wordStartPos = normalizedVertices.get(0);
                                        absoluteX = wordStartPos.getX() * PageSize.LETTER.getWidth();
                                        absoluteY = PageSize.LETTER.getHeight() - (wordStartPos.getY() * PageSize.LETTER.getHeight());
                                        hasCoords = true;
                                    }

                                    if (hasCoords) {
                                        cb.beginText();
                                        cb.setFontAndSize(baseFont, 9.0f);
                                        cb.setColorFill(Color.BLACK);
                                        cb.setTextMatrix(absoluteX, absoluteY);
                                        cb.showText(wordText.toString());
                                        cb.endText();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (renderedPageCount == 0) {
                throw new IllegalStateException(
                        "Cannot generate PDF because Vision API did not return any renderable OCR pages. " +
                                "The uploaded file may be empty, not a valid PDF, unreadable, or Vision returned no fullTextAnnotation data."
                );
            }

            document.close();

            byte[] pdfBytes = out.toByteArray();
            Path verificationPdfPath = null;
            if (writePdf) {
                verificationPdfPath = writeVerificationPdfArtifact(name, versionId, pdfBytes);
            }
            String content = fullText.toString().trim();

            org.springframework.ai.document.Document springDocument = new org.springframework.ai.document.Document(content);
            springDocument.getMetadata().put("name", name);
            springDocument.getMetadata().put("versionId", versionId);
            springDocument.getMetadata().put("source", "Vision API");
            springDocument.getMetadata().put("mimeType", "application/pdf");
            if (verificationPdfPath != null) {
                springDocument.getMetadata().put("verificationPdfPath", verificationPdfPath.toString());
            }
            springDocument.getMetadata().put("verificationPdfBytesLength", pdfBytes.length);
            springDocument.getMetadata().put("pageCount", renderedPageCount);
            springDocument.getMetadata().put("bucketName", bucketName);
            return new SpringDocumentConversion(springDocument, pdfBytes, verificationPdfPath);
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            throw new RuntimeException("Failed to generate PDF document from Vision API response", e);
        }
    }

    private Path writeVerificationPdfArtifact(String name, String versionId, byte[] pdfBytes) throws Exception {
        Path outputPath = Path.of(
                System.getProperty("user.dir"),
                "verification-" + name + "-" + versionId + ".pdf"
        ).toAbsolutePath();

        Files.write(outputPath, pdfBytes);
        return outputPath;
    }
}
