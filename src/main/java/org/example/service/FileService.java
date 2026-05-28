package org.example.service;

public interface FileService {
    String uploadFile(byte[] fileData, String fileName);

    byte[] getFile(String fileId);

    void deleteFile(String fileId);
}
