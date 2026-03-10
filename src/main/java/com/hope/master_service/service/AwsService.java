package com.hope.master_service.service;

import com.hope.master_service.exception.HopeException;

import java.util.UUID;

public interface AwsService {

    String uploadBase64(String base64Encoded, String folder, UUID identifier) throws HopeException;

    String getPreSignedUrl(String key) throws HopeException;

    void deleteObject(String key) throws HopeException;
}
