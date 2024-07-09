

package com.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.entity.CdrLog;
import com.entity.Response;
import com.entity.SDPPartner;
import com.lib.ConfigUtil;
import com.lib.Error;
import com.lib.StringUtils;
import com.lib.Utils;
import com.model.MSMProcess;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author KHANHDG
 * @since Sep 12, 2022
 */
public class FaceMatchService {

    private Logger log;
    protected Utils utils;
    protected MSMProcess model;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final MinIOService minIOService;

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public FaceMatchService() {
        log = LoggerFactory.getLogger(this.getClass());
        model = new MSMProcess();
        utils = new Utils();
        minIOService = new MinIOService();
    }

    public String process(long reqTime, Request baseRequest, HttpServletRequest request) {

        log.info("reqTime: " + reqTime + " Start call FaceMatchService.process");
        String result = processBase64(reqTime, baseRequest, request);
        log.info("reqTime: " + reqTime + " End call FaceMatchService.process");
        return result;
    }

    private String processBase64(long reqTime, Request baseRequest, HttpServletRequest request) {
        String token = baseRequest.getHeader("token");
        String method = request.getMethod().toUpperCase();
        String transId = null;
        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method + ", Token: " + token);
        CdrLog cdr = new CdrLog();
        try {
            log.info("reqTime: " + reqTime + " get remoteIp");
            String remoteIp = utils.getClientIpAddr(request);
            log.info("reqTime: " + reqTime + " remoteIp: " + remoteIp);

            log.info("reqTime: " + reqTime + " get remoteIp");
            String ipServer = utils.getIpServer(reqTime);
            log.info("reqTime: " + reqTime + " ipServer: " + ipServer);
            cdr.setIp(ipServer);

            cdr.setServiceCode(ConfigUtil.properties.getServiceFaceCompare());

            if (!"POST".equals(method)) {
                return buildBodyInsertLog(null, reqTime, cdr, Error.code.METHOD_WRONG, Error.message.METHOD_WRONG, null);
            }

            String bodyRequest = Utils.getBody(request, reqTime);
            if (StringUtils.isNullEmpty(bodyRequest)) {
                return buildBodyInsertLog(null, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null);
            }

            log.info("reqTime: " + reqTime + " start parse request body");
            JsonObject jsonRequest = new JsonParser().parse(bodyRequest).getAsJsonObject();
            log.info("reqTime: " + reqTime + " finish parse request body");
            transId = StringUtils.jsonToString(jsonRequest.get("transid"));
            String imgPath = StringUtils.jsonToString(jsonRequest.get("image_doc_path"));
            String imgSelfie = StringUtils.jsonToString(jsonRequest.get("image_selfie_base64"));
            log.info("reqTime: " + reqTime + " token:" + token + " transId:" + transId);

            if (StringUtils.isNullEmpty(token)
                    || StringUtils.isNullEmpty(transId)
                    || StringUtils.isNullEmpty(imgPath)
                    || StringUtils.isNullEmpty(imgSelfie)) {
                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.INVALID_PARAM, Error.message.INVALID_PARAM, null);
            }
            String imgSelfiePath = null;
            //todo: push to minio
            if (ConfigUtil.properties.isSave()) {
                String transIdFront = minIOService.getTransId(imgPath, transId);
                imgSelfiePath = minIOService.upload(imgSelfie, transIdFront, reqTime);
                if (StringUtils.isNullEmpty(imgSelfiePath)) {
                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.CANNOT_UPLOAD_IMAGE_MINIO, Error.message.CANNOT_UPLOAD_IMAGE_MINIO, null);
                }
            }

            JsonObject reqBodyLog = new JsonObject();
            reqBodyLog.addProperty("transId", transId);
            reqBodyLog.addProperty("image_doc_path", imgPath);
            reqBodyLog.addProperty("image_selfie_path", imgSelfiePath);

            log.info("reqTime: " + reqTime + " reqBodyLog:" + reqBodyLog);
            cdr.setRequestBody(reqBodyLog.toString());
            cdr.setReqOcrBody(reqBodyLog.toString());
            cdr.setReqOcrTime(dateFormat.format(new Date()));

            //check partner
            SDPPartner partner = model.getPartnerByToken(String.valueOf(reqTime), token);
            if (partner == null) {
                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.TOKEN_INVALID, Error.message.TOKEN_INVALID, null);
            }
            cdr.setPartnerId(partner.getId());
            cdr.setPartnerName(partner.getPartnerName());
            if (partner.getStatus() != 1) {
                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.USER_DEACTIVATE, Error.message.USER_DEACTIVATE, null);
            }

            String url = ConfigUtil.properties.getUrlFaceMatch();
            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("liveness", true);
            reqBody.addProperty("image_1", ConfigUtil.properties.getMinIOBucket() + "/" + imgSelfiePath);
            reqBody.addProperty("image_2", ConfigUtil.properties.getMinIOBucket() + "/" + imgPath);

            String result = utils.postRequest(String.valueOf(reqTime), url, reqBody.toString());
            if (StringUtils.isNullEmpty(result)) {
                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null);
            }
            cdr.setResOcrBody(result);

            JsonObject jsonResponse = new JsonParser().parse(result).getAsJsonObject();
            String statusCode = StringUtils.jsonToString(jsonResponse.get("status_code"));
            String statusMess = StringUtils.jsonToString(jsonResponse.get("en_message"));
            cdr.setCardType("real");
            JsonObject buildResult;
            JsonObject buildResult2;
            switch (statusCode) {
                case "200":
                    String strMatch = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("match"));
                    String strPercentMatch = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("percent_match"));
                    Double percentMatch = StringUtils.isNullEmpty(strPercentMatch) ? null : Double.valueOf(strPercentMatch);

                    String strLiveness = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("liveness"));
                    Boolean isLiveness = StringUtils.isNullEmpty(strLiveness) ? null : Boolean.parseBoolean(strLiveness);
                    String strPercentLiveness = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("percent_liveness"));
                    Double percentLiveness = StringUtils.isNullEmpty(strPercentLiveness) ? null : Double.valueOf(strPercentLiveness);

                    Float percentConfig = utils.getPercentMatchConfig(transId, partner.getId());
                    if (percentConfig == null) {
                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.PERCENT_MATCH_NOT_CONFIG, Error.message.PERCENT_MATCH_NOT_CONFIG, null);
                    }
                    boolean isMatch = percentMatch != null && percentMatch >= percentConfig;

                    buildResult = new JsonObject();
                    buildResult.addProperty("match", isMatch);
                    buildResult.addProperty("percent_match", percentMatch);
                    buildResult.addProperty("liveness", isLiveness);
                    buildResult.addProperty("percent_liveness", percentLiveness);


                    buildResult2 = new JsonObject();
                    buildResult2.add("face", buildResult);

                    boolean isMinusQuota = ConfigUtil.properties.isMinusQuota();
                    if (isMinusQuota && isMatch) {
                        log.info("reqTime: " + reqTime + " start minus quota");
                        int quotaRemain = model.minusQuota(transId, partner.getId(), 1, ConfigUtil.properties.getServiceFaceCompare());
                        if (quotaRemain > 0) {
                            //set response
                            cdr.setQuotaStart(quotaRemain);
                            log.info("reqTime: " + reqTime + " end minus quota remain quota: " + quotaRemain);
                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, buildResult2.toString());
                        } else {
                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.NOT_ENOUGH_QUOTA, Error.message.NOT_ENOUGH_QUOTA, null);
                        }
                    } else {
                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, buildResult2.toString());
                    }
                default:
                    buildResult = new JsonObject();
                    buildResult.addProperty("match", false);
                    buildResult.addProperty("percent_match", 0.0);
                    buildResult.addProperty("liveness", false);
                    buildResult.addProperty("percent_liveness", 0.0);


                    buildResult2 = new JsonObject();
                    buildResult2.add("face", buildResult);
                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, buildResult2.toString());
            }
        } catch (Exception ex) {
            log.error("reqId: " + reqTime, ex);
            JsonObject buildResult = new JsonObject();
            buildResult.addProperty("match", false);
            buildResult.addProperty("percent_match", 0.0);
            buildResult.addProperty("liveness", false);
            buildResult.addProperty("percent_liveness", 0.0);


            JsonObject buildResult2 = new JsonObject();
            buildResult2.add("face", buildResult);
            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, buildResult2.toString());
        }
    }

    public String buildBodyInsertLog(String transId, long reqTime, CdrLog cdr, String errCode, String errMessage, String result) {
        log.info("reqTime: " + reqTime + " build response");

        Response response = new Response(errCode, errMessage, result);
        String output = response.buildResponse();
        String outputLog = output;
        outputLog = utils.replaceLogBase64(outputLog, "image_base64");
        cdr.setRequestTime(dateFormat.format(reqTime));
        long resTime = System.currentTimeMillis();
       // cdr.setRES_OCR_BODY(outputLog);
        cdr.setResOcrTime(dateFormat.format(resTime));
        cdr.setResponseTime(dateFormat.format(resTime));
        cdr.setResponseBody(outputLog);
        cdr.setResultMessage(errMessage);
        if (Error.code.SUCCESS.equals(errCode)) {
            JsonObject jo = new JsonParser().parse(result).getAsJsonObject();
            Boolean isMatched = StringUtils.jsonToBoolean(jo.get("face").getAsJsonObject().get("match"));
            if (isMatched) {
                cdr.setStatus(1);
                cdr.setResultCode(Error.code.SUCCESS);
            } else {
                cdr.setStatus(0);
                cdr.setResultCode(Error.code.FAKE);
                cdr.setResultMessage(Error.message.FAKE);
            }
        } else {
            cdr.setStatus(0);
            cdr.setResultCode(errCode);
        }

        //if (!cdr.getIP().equals("127.0.0.1") && !cdr.getIP().equals("0:0:0:0:0:0:0:1")) {
        model.insert_cdr_log(transId, cdr);
        //}

        log.info("reqTime: " + reqTime + " response: " + outputLog);
        log.info("reqTime: " + reqTime + " end build response ----- " + (resTime - reqTime) + "ms");

        return output;
    }

}
