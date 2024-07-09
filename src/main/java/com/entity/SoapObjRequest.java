package com.entity;

import lombok.Data;

@Data
public class SoapObjRequest {
    private String url;
    private String accessKey;
    private String userName;
    private String password;
    private String token;
    private String dealerIsdn;
    private String idNo;
}
