package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.service.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private IFileService fileService;

    /**
     * 上传文件
     * POST /api/file/upload
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") String mediaType) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String fileName = fileService.upload(file, mediaType);
        String fileUrl = fileService.getFileUrl(fileName);
        return Result.success(Map.of("url", fileUrl));
    }
}
