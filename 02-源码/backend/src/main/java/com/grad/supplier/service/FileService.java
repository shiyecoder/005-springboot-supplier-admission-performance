package com.grad.supplier.service;

import com.grad.supplier.common.BizException;
import com.grad.supplier.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传服务。文件落到项目根目录 uploads/ 下按月份分目录，
 * 数据库只存相对路径 /uploads/...，前端通过静态映射访问，禁止外链。
 */
@Slf4j
@Service
public class FileService {

    private static final List<String> ALLOW = List.of("jpg", "jpeg", "png", "pdf");
    private static final long MAX_SIZE = 20 * 1024 * 1024L;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOW.contains(ext)) {
            throw new BizException("不支持的文件类型：" + ext + "，仅允许 jpg/jpeg/png/pdf");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("文件超过 20MB 限制");
        }
        try {
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String sub = (bizType == null || bizType.isBlank() ? "common" : bizType);
            Path dir = Paths.get(uploadDir, sub, month).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("url", "/uploads/" + sub + "/" + month + "/" + name);
            m.put("fileName", original);
            m.put("size", file.getSize());
            m.put("uploader", UserContext.get() == null ? "system" : UserContext.get().realName());
            m.put("contentType", file.getContentType());
            return m;
        } catch (Exception e) {
            log.error("文件保存失败", e);
            throw new BizException("文件保存失败：" + e.getMessage());
        }
    }
}
