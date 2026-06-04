package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

@Component
public class FileServiceImpl implements FileService{

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public FileServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(byte[] fileData, String fileName) {
        PutObjectResponse putObjectResponse = s3Client.putObject(builder -> builder.bucket(bucketName).key(fileName).build(), RequestBody.fromBytes(fileData));
        return putObjectResponse.versionId();
    }

    @Override
    public byte[] getFile(String fileId) {
        try {
            return s3Client.getObject(builder -> builder.bucket(bucketName).key(fileId).build()).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String fileId) {
        s3Client.deleteObject(builder -> builder.bucket(bucketName).key(fileId).build());
    }
}
