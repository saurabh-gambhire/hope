package com.hope.master_service.service;

import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class AwsServiceImpl extends AppService implements AwsService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${spring.profiles.active}")
    private String profile;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public AwsServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String uploadBase64(String base64Encoded, String folder, UUID identifier) throws HopeException {
        if (StringUtils.isBlank(base64Encoded)) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
            String extension = detectFileExtension(decodedBytes);
            String key = buildPath(folder, identifier, extension);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromBytes(decodedBytes));
            if (response != null && response.sdkHttpResponse().isSuccessful()) {
                log.info("Uploaded file to S3: {}", key);
                return key;
            }

            throwError(ResponseCode.AWS_ERROR, "Failed to upload file to S3");
        } catch (HopeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading to S3: {}", e.getMessage(), e);
            throwError(ResponseCode.AWS_ERROR, e.getMessage());
        }
        return null;
    }

    @Override
    public String getPreSignedUrl(String key) throws HopeException {
        if (StringUtils.isBlank(key)) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Error generating pre-signed URL for key {}: {}", key, e.getMessage(), e);
            throwError(ResponseCode.AWS_ERROR, e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteObject(String key) throws HopeException {
        if (StringUtils.isBlank(key)) {
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.info("Deleted S3 object: {}", key);
        } catch (Exception e) {
            log.error("Error deleting S3 object {}: {}", key, e.getMessage(), e);
            throwError(ResponseCode.AWS_ERROR, e.getMessage());
        }
    }

    private String buildPath(String folder, UUID identifier, String extension) {
        return profile + "/" + folder + "/" + identifier + "/" + UUID.randomUUID() + extension;
    }

    private String detectFileExtension(byte[] fileBytes) {
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            String mimeType = URLConnection.guessContentTypeFromStream(is);
            if (mimeType != null) {
                String[] parts = mimeType.split("/");
                return "." + parts[1];
            }
        } catch (Exception e) {
            log.warn("Could not detect file extension, defaulting to .png");
        }
        return ".png";
    }
}
