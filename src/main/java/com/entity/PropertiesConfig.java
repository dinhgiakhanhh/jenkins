package com.entity;

import lombok.Data;

@Data
public class PropertiesConfig {

    private String host;
    private String port;
    private long timeOut;
    private boolean isSave;
    private String urlAPIGW;
    private String urlOcrBase64;
    private String urlFaceMatch;
    private String urlPassportBase64;
    private boolean isTranslate;
    private String keyTranslate;
    private boolean isMinusQuota;
    private String ipServer;
    private String urlTranslate;
    private String minIOKey;
    private String minIOPass;
    private String minIOUrl;
    private String minIOBucket;
    private String provinceList;
    private String serviceId;
    private String servicePassport;
    private String serviceFaceCompare;
    private boolean substringIdNumber;
    private long maxFileSize;
    private long maxRequestSize;
    private int fileSizeThreshold;
    private String thresholdFolder;
    private String urlIdMinio;
    private String urlPassportMinio;
    private String errNotPassed;
    private boolean isEditable;
    private boolean isRecapture;

}
