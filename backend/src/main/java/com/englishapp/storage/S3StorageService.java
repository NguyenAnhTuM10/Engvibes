package com.englishapp.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.storage.bucket-videos}")
    private String videosBucket;

    @Value("${app.storage.bucket-audios}")
    private String audiosBucket;

    @PostConstruct
    public void initBuckets() {
        initBucket(videosBucket);
        initBucket(audiosBucket);
    }

    private void initBucket(String bucket) {
        try {
            s3Client.headBucket(r -> r.bucket(bucket));
            log.debug("Bucket exists: {}", bucket);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(r -> r.bucket(bucket));
            log.info("Created bucket: {}", bucket);
        }
    }

    @Override
    public String upload(String bucket, String key, InputStream content, long size, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(size)
                        .build(),
                RequestBody.fromInputStream(content, size));
        log.debug("Uploaded to {}/{}", bucket, key);
        return key;
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, Duration expiry) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public byte[] download(String bucket, String key) {
        return s3Client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asByteArray();
    }

    @Override
    public void delete(String bucket, String key) {
        s3Client.deleteObject(r -> r.bucket(bucket).key(key));
        log.debug("Deleted {}/{}", bucket, key);
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            s3Client.headObject(r -> r.bucket(bucket).key(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
