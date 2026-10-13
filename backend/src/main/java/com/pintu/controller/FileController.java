package com.pintu.controller;

import com.pintu.service.storage.ObjectStorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final ObjectStorageProvider storageProvider;

    @GetMapping("/{folder}/{owner}/{filename:.+}")
    public ResponseEntity<Resource> get(
            @PathVariable String folder,
            @PathVariable String owner,
            @PathVariable String filename) {
        if (!folder.matches("[a-zA-Z0-9_-]+")
                || !owner.matches("[a-zA-Z0-9_-]+")
                || !filename.matches("[a-zA-Z0-9._-]+")) {
            return ResponseEntity.badRequest().build();
        }
        Resource resource = storageProvider.get(folder + "/" + owner + "/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}

