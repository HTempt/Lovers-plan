package com.lovers.service.impl;

import com.lovers.common.exception.BusinessException;
import com.lovers.service.IFileService;
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
public class FileServiceImpl implements IFileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 对外公网文件地址前缀（Nginx 反代到 MinIO，bucket 公共读），
     * 例如 http://192.144.130.3/files 。为空时退回预签名URL（要求客户端能直连MinIO）。
     */
    @Value("${minio.public-endpoint:}")
    private String publicEndpoint;

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
     * - null/空 → null
     * - MinIO完整URL（含内网endpoint）→ 提取对象路径后转公网URL
     * - 第三方完整URL（WeChat等）→ 直接返回
     * - 原始对象路径 → 转公网URL
     *
     * 说明：文件公网访问走 Nginx 反代 /files/ → MinIO（bucket 公共读）。
     * 预签名URL签名中包含内网host，经代理后无法校验，故仅在未配置
     * minio.public-endpoint 时作为回退。
     */
    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;

        // 提取MinIO对象路径（兼容旧存储值：内网完整URL / 预签名URL）
        String minioPrefix = endpoint + "/" + bucketName + "/";
        if (filePath.startsWith(minioPrefix)) {
            String pathWithParams = filePath.substring(minioPrefix.length());
            int queryIdx = pathWithParams.indexOf('?');
            String objectPath = queryIdx > 0 ? pathWithParams.substring(0, queryIdx) : pathWithParams;
            return toPublic(objectPath);
        }

        // 第三方完整URL（WeChat头像等）→ 直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }

        // 原始MinIO对象路径 → 公网URL
        return toPublic(filePath);
    }

    /** 把对象路径转成客户端可访问的URL */
    private String toPublic(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) return null;
        if (publicEndpoint != null && !publicEndpoint.isEmpty()) {
            return publicEndpoint + "/" + objectPath;
        }
        // 回退：预签名URL（要求客户端能直连MinIO）
        return generatePresignedUrl(objectPath);
    }

    /** 生成MinIO预签名URL */
    private String generatePresignedUrl(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) return null;
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build());
            return url;
        } catch (Exception e) {
            log.warn("Failed to get presigned URL for: {}", objectPath);
            return endpoint + "/" + bucketName + "/" + objectPath;
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
