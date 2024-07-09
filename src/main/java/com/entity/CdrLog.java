package com.entity;

import lombok.Data;

@Data
public class CdrLog {

    private Long id;
    private Long partnerId;
    private String partnerName;
    private String serviceCode;
    private String requestTime;
    private String responseTime;
    private String requestBody;
    private String responseBody;
    private String resultCode;
    private String resultMessage;
    private int status;
    private String reqOcrBody;
    private String resOcrBody;
    private String reqOcrTime;
    private String resOcrTime;
    private String transid;
    private int quotaStart;
    private String cardType;
    private String side;
    private String ip;
}
