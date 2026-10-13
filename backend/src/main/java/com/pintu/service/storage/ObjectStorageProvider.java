package com.pintu.service.storage;

import org.springframework.core.io.Resource;

public interface ObjectStorageProvider {

    String put(String key, byte[] content, String contentType);

    Resource get(String key);
}

