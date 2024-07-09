package com.service;

import com.entity.CdrLog;
import com.entity.ResponseDto;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lib.ConfigUtil;
import com.lib.Error;
import com.lib.StringUtils;
import com.lib.Utils;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class FaceMatchServiceV2 {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    protected Utils utils;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final MinIOService minIOService;

    public FaceMatchServiceV2() {
        utils = new Utils();
        minIOService = new MinIOService();
    }

    public String process(long reqTime, Request baseRequest, HttpServletRequest request) {
        String token = baseRequest.getHeader("token");
        String method = request.getMethod().toUpperCase();
        String transId = null;
        String imageDocPath = null;
        String imgSelfiePath;
        JsonObject buildResult;
        CommonService commonService = new CommonService();

        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method + ", Token: " + token);
        CdrLog cdr = new CdrLog();
        cdr.setRequestTime(dateFormat.format(new Date(reqTime)));

        String remoteIp = utils.getClientIpAddr(request);
        log.info("reqTime: " + reqTime + " remoteIp: " + remoteIp);

        String ipServer = utils.getIpServer(reqTime);
        log.info("reqTime: " + reqTime + " ipServer: " + ipServer);
        cdr.setIp(ipServer);
        cdr.setServiceCode(ConfigUtil.properties.getServiceFaceCompare());

        if (!"POST".equals(method)) {
            return commonService.buildResponse(cdr, ResponseDto.builder()
                    .reqTime(reqTime)
                    .errCode(Error.code.METHOD_WRONG)
                    .errMessage(Error.message.METHOD_WRONG)
                    .errMessageKh(Error.message_kh.METHOD_WRONG)
                    .build());
        }

        //todo: get request body
        InputStream inputStream = null;
        try {
            Collection<Part> parts = request.getParts();
            String fileName = null;
            for (Part part : parts) {
                if (utils.isFilePart(part)) {
                    fileName = utils.extractFileName(part);
                    inputStream = part.getInputStream();
                } else {
                    transId = request.getParameter("transid");
                    imageDocPath = request.getParameter("image_doc_path");
                }
            }

            if (!StringUtils.isNullEmpty(fileName)
                    && inputStream != null
                    && !StringUtils.isNullEmpty(transId)) {

                //todo: validate token
                String result = commonService.validateToken(reqTime, token, transId, cdr);
                if (!StringUtils.isNullEmpty(result)) {
                    return result;
                }

                //todo: upload to minio
                String transIdFront = minIOService.getTransId(imageDocPath, transId);
                imgSelfiePath = minIOService.upload(inputStream, transIdFront, reqTime, "self");
                if (StringUtils.isNullEmpty(imgSelfiePath)) {
                    return commonService.buildResponse(cdr, ResponseDto.builder()
                            .transId(transId)
                            .reqTime(reqTime)
                            .errCode(Error.code.CANNOT_UPLOAD_IMAGE_MINIO)
                            .errMessage(Error.message.CANNOT_UPLOAD_IMAGE_MINIO)
                            .errMessageKh(Error.message_kh.CANNOT_UPLOAD_IMAGE_MINIO)
                            .build());
                }

                if(StringUtils.isNullEmpty(imageDocPath)){
                    return commonService.buildResponse(cdr, ResponseDto.builder()
                            .transId(transId)
                            .reqTime(reqTime)
                            .errCode(Error.code.MISSING_DOCUMENT_IMAGE)
                            .errMessage(Error.message.MISSING_DOCUMENT_IMAGE)
                            .errMessageKh(Error.message_kh.MISSING_DOCUMENT_IMAGE)
                            .build());
                }

                JsonObject reqBody = new JsonObject();
                reqBody.addProperty("transId", transId);
                reqBody.addProperty("image_doc_path", imageDocPath);
                reqBody.addProperty("image_selfie_path", imgSelfiePath);

                cdr.setRequestBody(reqBody.toString());

                String url = ConfigUtil.properties.getUrlFaceMatch();
                reqBody = new JsonObject();
                reqBody.addProperty("liveness", true);
                reqBody.addProperty("image_1", ConfigUtil.properties.getMinIOBucket() + "/" + imgSelfiePath);
                reqBody.addProperty("image_2", ConfigUtil.properties.getMinIOBucket() + "/" + imageDocPath);
                cdr.setReqOcrBody(reqBody.toString());
                cdr.setReqOcrTime(dateFormat.format(new Date()));

                result = utils.postRequest(String.valueOf(reqTime), url, reqBody.toString());

                if (StringUtils.isNullEmpty(result)) {
                    return commonService.buildResponse(cdr, ResponseDto.builder()
                            .transId(transId)
                            .reqTime(reqTime)
                            .errCode(Error.code.OCR_FAIL)
                            .errMessage(Error.message.OCR_FAIL)
                            .errMessageKh(Error.message_kh.OCR_FAIL)
                            .build());
                }
                cdr.setResOcrBody(result);
                cdr.setResOcrTime(dateFormat.format(new Date()));

                JsonObject jsonResponse = new JsonParser().parse(result).getAsJsonObject();
                String statusCode = StringUtils.jsonToString(jsonResponse.get("status_code"));
                String statusMess = StringUtils.jsonToString(jsonResponse.get("en_message"));
                cdr.setCardType("real");
                if ("200".equals(statusCode)) {
                    String strPercentMatch = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("percent_match"));
                    Double percentMatch = StringUtils.isNullEmpty(strPercentMatch) ? null : Double.valueOf(strPercentMatch);

                    String strLiveness = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("liveness"));
                    Boolean isLiveness = StringUtils.isNullEmpty(strLiveness) ? null : Boolean.parseBoolean(strLiveness);
                    String strPercentLiveness = StringUtils.jsonToString(jsonResponse.get("data").getAsJsonObject().get("face").getAsJsonObject().get("percent_liveness"));
                    Double percentLiveness = StringUtils.isNullEmpty(strPercentLiveness) ? null : Double.valueOf(strPercentLiveness);

                    Float percentConfig = utils.getPercentMatchConfig(transId, cdr.getPartnerId());
                    boolean isMatch = percentMatch != null && percentMatch >= (percentConfig == null ? 20.0 : percentConfig);

                    buildResult = new JsonObject();
                    buildResult.addProperty("match", isMatch);
                    buildResult.addProperty("percent_match", percentMatch);
                    buildResult.addProperty("liveness", isLiveness);
                    buildResult.addProperty("percent_liveness", percentLiveness);

                    if (isMatch) {
                        return commonService.buildResponse(cdr, ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.SUCCESS)
                                .errMessage(Error.message.SUCCESS)
                                .errMessageKh(Error.message_kh.SUCCESS)
                                .value(buildResult)
                                .build());
                    } else {
                        return commonService.buildResponse(cdr, ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.FAKE)
                                .errMessage(Error.message.FAKE)
                                .errMessageKh(Error.message_kh.FAKE)
                                .value(buildResult)
                                .build());
                    }
                } else {
                    return commonService.buildResponse(cdr, ResponseDto.builder()
                            .transId(transId)
                            .reqTime(reqTime)
                            .errCode(statusCode)
                            .errMessage(statusMess)
                            .build());
                }
            } else {
                return commonService.buildResponse(cdr, ResponseDto.builder()
                        .reqTime(reqTime)
                        .transId(transId)
                        .errCode(Error.code.INVALID_PARAM)
                        .errMessage(Error.message.INVALID_PARAM)
                        .errMessageKh(Error.message_kh.INVALID_PARAM)
                        .build());
            }
        } catch (Exception ex) {
            log.error("reqTime: " + reqTime, ex);
            return commonService.buildResponse(cdr, ResponseDto.builder()
                    .reqTime(reqTime)
                    .errCode(Error.code.SYSTEM_BUSY)
                    .errMessage(Error.message.SYSTEM_BUSY)
                    .errMessageKh(Error.message_kh.SYSTEM_BUSY)
                    .build());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("reqTime: " + reqTime, e);
                }
            }
        }
    }

}
