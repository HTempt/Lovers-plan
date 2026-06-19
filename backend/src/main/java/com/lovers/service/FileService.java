package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件到MinIO
     */
    public String upload(MultipartFile file, String mediaType) {
        String fileName = generateFileName(mediaType);
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return fileName;
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new BusinessException("文件上传失败");
        }
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String filePath) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .method(Method.GET)
                            .expiry(24, TimeUnit.HOURS)
                            .build());
            return url;
        } catch (Exception e) {
            log.warn("Failed to get file URL: {}", filePath);
            return endpoint + "/" + bucketName + "/" + filePath;
        }
    }

    /**
     * 删除文件
     */
    public void delete(String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete file: {}", filePath);
        }
    }

    private String generateFileName(String mediaType) {
        String ext;
        switch (mediaType) {
            case "image" -> ext = ".jpg";
            case "video" -> ext = ".mp4";
            case "audio" -> ext = ".m4a";
            default -> ext = ".dat";
        }
        return mediaType + "/" + UUID.randomUUID() + ext;
    }
}
