package com.service;


import com.lib.ConfigUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class MinIOService {
    private final Logger log = LoggerFactory.getLogger(MinIOService.class);
    private final String folder;
    private static MinioClient minioClient;

    public MinIOService() {
        folder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (minioClient == null) {
            try {
                minioClient = MinioClient.builder()
                        .endpoint(ConfigUtil.properties.getMinIOUrl())
                        .credentials(ConfigUtil.properties.getMinIOKey(), ConfigUtil.properties.getMinIOPass())
                        .build();
            } catch (Exception ex) {
                log.error("MinIOService: ", ex);
            }
        }
    }

    public boolean bucketExists() {
        if (minioClient != null) {
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(ConfigUtil.properties.getMinIOBucket()).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(ConfigUtil.properties.getMinIOBucket()).build());
                }
                return true;
            } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                log.error("bucketExists: ", e);
            }
        }
        return false;
    }

    public String upload(String base64, String transId, long reqTime) {
        long start = System.currentTimeMillis();

        String pathImg = null;
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

        if (bucketExists()) {
            pathImg = folder + "/" + transId + "/" + System.nanoTime() + ".jpg";
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(ConfigUtil.properties.getMinIOBucket())
                                .object(pathImg)
                                .stream(inputStream, imageBytes.length, -1)
                                .contentType("image/jpeg")
                                .build()
                );
            } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                log.error("reqTime: " + reqTime, e);
                pathImg = null;
            }
        }
        log.info("reqTime: " + reqTime + " -- Save image time:" + (System.currentTimeMillis() - start) + "ms");

        return pathImg;
    }

    public String upload(InputStream input, String transId, long reqTime, String side) {
        long start = System.currentTimeMillis();

        String pathImg = null;
        if (bucketExists()) {
            pathImg = folder + "/" + transId + "/" + System.nanoTime() + "_" + side + ".jpg";
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(ConfigUtil.properties.getMinIOBucket())
                                .object(pathImg)
                                .stream(input, input.available(), -1)
                                .contentType("image/jpeg")
                                .build()
                );
            } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                log.error("reqTime: " + reqTime, e);
                pathImg = null;
            }
        }
        log.info("reqTime: " + reqTime + " -- Save image time:" + (System.currentTimeMillis() - start) + "ms" + " -- " + pathImg);

        return pathImg;
    }

    public String getTransId(String pathImage, String transId){
        try{
            String str = pathImage.trim().substring(0, pathImage.lastIndexOf("/"));
            return str.substring(str.lastIndexOf("/") + 1);
        }catch (Exception ex){
            //log.error("transId: " + transId, ex);
        }
        return transId;
    }
}
