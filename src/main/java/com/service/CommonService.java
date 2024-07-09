package com.service;


import com.entity.CdrLog;
import com.entity.ResponseDto;
import com.entity.SDPPartner;
import com.google.gson.JsonObject;
import com.lib.ConfigUtil;
import com.lib.Error;
import com.model.MSMProcess;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

@Data
public class CommonService {
    private final Logger log = LoggerFactory.getLogger(CommonService.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private MSMProcess partnerService;

    public CommonService() {
        partnerService = new MSMProcess();
    }

    public String validateToken(long reqTime, String token, String transId, CdrLog cdr) {
        long start = System.currentTimeMillis();
        log.info("reqTime: " + reqTime + " -- validate token --");
        String result = null;
        SDPPartner partner = partnerService.getPartnerByToken(String.valueOf(reqTime), token);
        if (partner != null) {
            cdr.setPartnerId(partner.getId());
            cdr.setPartnerName(partner.getPartnerName());
            if (partner.getStatus() != 1) {
                result = buildResponse(cdr, ResponseDto.builder()
                        .reqTime(reqTime)
                        .transId(transId)
                        .errCode(Error.code.USER_DEACTIVATE)
                        .errMessage(Error.message.USER_DEACTIVATE)
                        .errMessageKh(Error.message_kh.USER_DEACTIVATE)
                        .build());
            }
        } else {
            result = buildResponse(cdr, ResponseDto.builder()
                    .reqTime(reqTime)
                    .transId(transId)
                    .errCode(Error.code.TOKEN_INVALID)
                    .errMessage(Error.message.TOKEN_INVALID)
                    .errMessageKh(Error.message_kh.TOKEN_INVALID)
                    .build());
        }

        log.info("reqTime: " + reqTime + " -- validate token finished " + (System.currentTimeMillis() - start) + "ms");
        return result;
    }

    public String buildResponse(CdrLog cdr, ResponseDto response) {
        log.info("reqTime: " + response.getReqTime() + " build response");
        long resTime = System.currentTimeMillis();

        JsonObject finalResult = new JsonObject();
        finalResult.addProperty("transid", response.getTransId());
        boolean isPassed = true;
        for (String errCode : ConfigUtil.properties.getErrNotPassed().split(",")) {
            if (errCode.equals(response.getErrCode())) {
                isPassed = false;
                break;
            }
        }
        finalResult.addProperty("passed", isPassed);

        if (ConfigUtil.properties.getServiceFaceCompare().equals(cdr.getServiceCode())) {
            finalResult.addProperty("gw_status", response.getErrCode());
            finalResult.addProperty("gw_message", response.getErrMessage());
            finalResult.addProperty("gw_message_kh", response.getErrMessageKh());
            JsonObject detailResult = new JsonObject();
            detailResult.add("face", response.getValue());
            finalResult.add("gw_body", detailResult);
        } else {
            finalResult.addProperty("code", response.getErrCode());
            finalResult.addProperty("message", response.getErrMessage());
            finalResult.addProperty("message_kh", response.getErrMessageKh());
            finalResult.add("confidence", response.getConfidence());
            finalResult.add("value", response.getValue());
        }

        cdr.setResponseTime(dateFormat.format(resTime));
        cdr.setResponseBody(finalResult.toString());
        cdr.setResultMessage(response.getErrMessage());
        if (Error.code.SUCCESS.equals(response.getErrCode())) {
            cdr.setStatus(1);
            cdr.setResultCode(Error.code.SUCCESS);
        } else {
            cdr.setStatus(0);
            cdr.setResultCode(response.getErrCode());
        }

        partnerService.insert_cdr_log(response.getTransId(), cdr);

        log.info("reqTime: " + response.getReqTime() + " response: " + finalResult);
        return finalResult.toString();
    }

    public String buildResponse(ResponseDto response) {
        log.info("reqTime: " + response.getReqTime() + " build response");

        JsonObject finalResult = new JsonObject();
        finalResult.addProperty("code", response.getErrCode());
        finalResult.addProperty("message", response.getErrMessage());
        finalResult.addProperty("message_kh", response.getErrMessageKh());
        finalResult.addProperty("transid", response.getTransId());
        finalResult.add("value", response.getValue());

        log.info("reqTime: " + response.getReqTime() + " response: " + finalResult);

        return finalResult.toString();
    }
}
