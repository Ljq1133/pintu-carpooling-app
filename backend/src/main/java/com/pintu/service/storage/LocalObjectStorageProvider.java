package com.pintu.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalObjectStorageProvider implements ObjectStorageProvider {

    private final Path root;

    public LocalObjectStorageProvider(@Value("${storage.local.root:./data/objects}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return "/api/files/" + key.replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store object", ex);
        }
    }

    @Override
    public Resource get(String key) {
        return new FileSystemResource(resolve(key));
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return target;
    }
}

