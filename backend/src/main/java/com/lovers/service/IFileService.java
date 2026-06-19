package com.lovers.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    String upload(MultipartFile file, String mediaType);
    String getFileUrl(String filePath);
    void delete(String filePath);
}
