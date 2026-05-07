package com.englishapp.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {
    String upload(String bucket, String key, InputStream content, long size, String contentType);
    String generatePresignedUrl(String bucket, String key, Duration expiry);
    void delete(String bucket, String key);
    boolean exists(String bucket, String key);
}
