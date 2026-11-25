package com.grad.supplier.controller;

import com.grad.supplier.common.R;
import com.grad.supplier.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 文件上传：证照、现场照片、头像统一走这里，返回相对路径。 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) String bizType) {
        return R.ok(fileService.upload(file, bizType));
    }
}
