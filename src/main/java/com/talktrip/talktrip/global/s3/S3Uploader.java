package com.talktrip.talktrip.global.s3;

import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.S3Excepttion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dirName) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String fileName = dirName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(fileName)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new S3Excepttion(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
    }

    public void delete(String fileUrl) {
        String fileKey = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
        s3Client.deleteObject(builder -> builder.bucket(bucket).key(fileKey));
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            String key = extractKeyFromUrl(fileUrl);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            throw new S3Excepttion(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        String[] parts = fileUrl.split(".amazonaws.com/");
        return parts.length > 1 ? parts[1] : fileUrl;
    }

    public String calculateHash(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192]; // 8KB 버퍼
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hash = digest.digest();
            return Base64.getEncoder().encodeToString(hash);

        } catch (IOException e) {
            throw new S3Excepttion(ErrorCode.IMAGE_UPLOAD_FAILED);
        } catch (NoSuchAlgorithmException e) {
            throw new S3Excepttion(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }


}