package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.common.exception.BusinessException;
import com.pintu.common.util.IdGenerator;
import com.pintu.dto.AttachmentUploadVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.storage.ObjectStorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportAttachmentController {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final ObjectStorageProvider storageProvider;

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AttachmentUploadVO> upload(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(1002, "附件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(1002, "附件不能超过 5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new BusinessException(1002, "仅支持图片附件");
        }

        String userId = CurrentUser.id(authentication);
        String originalName = sanitize(file.getOriginalFilename());
        String extension = extensionOf(originalName, contentType);
        String key = "support/" + sanitize(userId) + "/" + IdGenerator.next("att") + extension;
        String publicPath = storageProvider.put(key, file.getBytes(), contentType);

        AttachmentUploadVO vo = new AttachmentUploadVO();
        vo.setAttachmentKey(publicPath);
        vo.setFileName(originalName);
        vo.setContentType(contentType);
        vo.setSize(file.getSize());
        return Result.ok(vo);
    }

    private String sanitize(String value) {
        String normalized = value == null || value.isBlank() ? "file" : value.trim();
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extensionOf(String fileName, String contentType) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            return fileName.substring(dot).toLowerCase(Locale.ROOT);
        }
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        return ".jpg";
    }
}
