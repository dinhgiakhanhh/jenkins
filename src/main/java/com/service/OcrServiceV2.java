
package com.service;

import com.entity.CdrLog;
import com.entity.ResponseDto;
import com.entity.SoapObjRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lib.*;
import com.lib.Error;
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

public class OcrServiceV2 {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    protected Utils utils;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final MinIOService minIOService;

    public OcrServiceV2() {
        utils = new Utils();
        minIOService = new MinIOService();
    }

    public String process(long reqTime, Request baseRequest, HttpServletRequest request, String docType) {
        String transId = null;
        String urlMet = null;
        String accessKeyMet = null;
        String userNameMet = null;
        String passwordMet = null;
        String tokenMet = null;
        String dealerIsdnMet = null;
        String token = baseRequest.getHeader("token");
        String method = request.getMethod().toUpperCase();
        String pathImage;
        CommonService commonService = new CommonService();

        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method + ", Token: " + token + ", Doc_type: " + docType);
        CdrLog cdr = new CdrLog();
        cdr.setRequestTime(dateFormat.format(new Date(reqTime)));
        cdr.setCardType("real");

        String ipServer = utils.getIpServer(reqTime);
        log.info("reqTime: " + reqTime + " ipServer: " + ipServer);
        cdr.setIp(ipServer);

        if (!"POST".equals(method)) {
            return commonService.buildResponse(cdr, ResponseDto.builder()
                    .reqTime(reqTime)
                    .errCode(Error.code.METHOD_WRONG)
                    .errMessage(Error.message.METHOD_WRONG)
                    .errMessageKh(Error.message_kh.METHOD_WRONG)
                    .build());
        } else {
            InputStream inputStream = null;
            try {
                //todo: get request body
                Collection<Part> parts = request.getParts();
                String fileName = null;
                for (Part part : parts) {
                    if (!StringUtils.isNullEmpty(fileName)
                            && inputStream != null
                            && !StringUtils.isNullEmpty(transId)
                            && !StringUtils.isNullEmpty(urlMet)
                            && !StringUtils.isNullEmpty(accessKeyMet)
                            && !StringUtils.isNullEmpty(userNameMet)
                            && !StringUtils.isNullEmpty(passwordMet)
                            && !StringUtils.isNullEmpty(tokenMet)
                            && !StringUtils.isNullEmpty(dealerIsdnMet)) {
                        break;
                    }
                    if (utils.isFilePart(part)) {
                        fileName = utils.extractFileName(part);
                        inputStream = part.getInputStream();
                    } else {
                        transId = request.getParameter("transid");
                        urlMet = request.getParameter("url");
                        accessKeyMet = request.getParameter("accessKey");
                        userNameMet = request.getParameter("userName");
                        passwordMet = request.getParameter("password");
                        tokenMet = request.getParameter("token");
                        dealerIsdnMet = request.getParameter("dealerIsdn");
                    }
                }

                if (!StringUtils.isNullEmpty(fileName)
                        && inputStream != null
                        && !StringUtils.isNullEmpty(transId)
                        && !StringUtils.isNullEmpty(urlMet)
                        && !StringUtils.isNullEmpty(accessKeyMet)
                        && !StringUtils.isNullEmpty(userNameMet)
                        && !StringUtils.isNullEmpty(passwordMet)
                        && !StringUtils.isNullEmpty(tokenMet)
                        && !StringUtils.isNullEmpty(dealerIsdnMet)
                ) {
                    //todo: validate document
                    String url = null;
                    switch (docType) {
                        case Constant.DOC_TYPE_ID:
                            cdr.setServiceCode(ConfigUtil.properties.getServiceId());
                            cdr.setSide("front");
                            url = ConfigUtil.properties.getUrlIdMinio();
                            break;
                        case Constant.DOC_TYPE_PP:
                            cdr.setServiceCode(ConfigUtil.properties.getServicePassport());
                            url = ConfigUtil.properties.getUrlPassportMinio();
                            break;
                    }

                    //todo: validate token
                    String result = commonService.validateToken(reqTime, token, transId, cdr);
                    if (!StringUtils.isNullEmpty(result)) {
                        return result;
                    }

                    //todo: upload to minio
                    pathImage = minIOService.upload(inputStream, transId, reqTime, "front");
                    if (StringUtils.isNullEmpty(pathImage)) {
                        return commonService.buildResponse(cdr, ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.CANNOT_UPLOAD_IMAGE_MINIO)
                                .errMessage(Error.message.CANNOT_UPLOAD_IMAGE_MINIO)
                                .errMessageKh(Error.message_kh.CANNOT_UPLOAD_IMAGE_MINIO)
                                .build());
                    }

                    //todo: build request ocr
                    JsonObject reqBody = new JsonObject();
                    reqBody.addProperty("transid", transId);
                    reqBody.addProperty("image", ConfigUtil.properties.getMinIOBucket() + "/" + pathImage);
                    cdr.setRequestBody(reqBody.toString());
                    cdr.setReqOcrBody(reqBody.toString());
                    cdr.setReqOcrTime(dateFormat.format(new Date()));

                    //todo: call api ocr
                    result = utils.postRequest(String.valueOf(reqTime), url, reqBody.toString());

                    cdr.setResOcrTime(dateFormat.format(new Date()));
                    cdr.setResOcrBody(result);

                    if (StringUtils.isNullEmpty(result)) {
                        return commonService.buildResponse(cdr, ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.OCR_FAIL)
                                .errMessage(Error.message.OCR_FAIL)
                                .errMessageKh(Error.message_kh.OCR_FAIL)
                                .build());
                    }

                    //todo: transform ocr result
                    JsonObject jsonResponse = null;
                    try {
                        JsonElement jsonElement = new JsonParser().parse(result);
                        if (jsonElement.isJsonArray()) {
                            jsonResponse = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
                        } else {
                            jsonResponse = jsonElement.getAsJsonObject();
                        }
                    } catch (Exception ex) {
                        log.error("reqTime: " + reqTime, ex);
                    }

                    if (jsonResponse == null) {
                        return commonService.buildResponse(cdr, ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.OCR_FAIL)
                                .errMessage(Error.message.OCR_FAIL)
                                .errMessageKh(Error.message_kh.OCR_FAIL)
                                .build());
                    }
                    ResponseDto responseDto = new ResponseDto();

                    //todo: build soap request
                    SoapObjRequest soapRequest = new SoapObjRequest();
                    soapRequest.setUrl(urlMet);
                    soapRequest.setAccessKey(accessKeyMet);
                    soapRequest.setToken(tokenMet);
                    soapRequest.setUserName(userNameMet);
                    soapRequest.setPassword(passwordMet);
                    soapRequest.setDealerIsdn(dealerIsdnMet);

                    switch (docType) {
                        case Constant.DOC_TYPE_ID:
                            responseDto = idProcess(reqTime, pathImage, transId, soapRequest, jsonResponse);
                            break;
                        case Constant.DOC_TYPE_PP:
                            responseDto = passportProcess(reqTime, pathImage, transId, soapRequest, jsonResponse);
                            break;
                    }

                    return commonService.buildResponse(cdr, responseDto);
                } else {
                    return commonService.buildResponse(cdr, ResponseDto.builder()
                            .reqTime(reqTime)
                            .transId(transId)
                            .errCode(Error.code.INVALID_PARAM)
                            .errMessage(Error.message.INVALID_PARAM)
                            .errMessageKh(Error.message_kh.INVALID_PARAM)
                            .build());
                }
            } catch (Exception e) {
                log.error("reqTime: " + reqTime, e);
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

    private ResponseDto idProcess(long reqTime, String pathImage, String transId, SoapObjRequest soapRequest, JsonObject jsonResponse) {
        long start = System.currentTimeMillis();
        ResponseDto responseDto;
        JsonObject confidenceRes = jsonResponse.get("confidence").getAsJsonObject();
        JsonObject valueRes = jsonResponse.get("value").getAsJsonObject();
        JsonObject fraud = jsonResponse.get("fraud_detect") != null && !jsonResponse.get("fraud_detect").isJsonNull() ? jsonResponse.get("fraud_detect").getAsJsonObject() : null;
        JsonObject fraudRes = new JsonObject();
        JsonObject edit = new JsonObject();
        if (ConfigUtil.properties.isEditable()) {
            edit.addProperty("check_edited", fraud != null ? StringUtils.jsonToBoolean(fraud.get("check_edited").getAsJsonObject().get("check_edited")) : null);
            edit.addProperty("confidence", fraud != null ? StringUtils.jsonToDouble(fraud.get("check_edited").getAsJsonObject().get("confidence")) : null);
        } else {
            edit.addProperty("check_edited", false);
            edit.addProperty("confidence", 100.0);
        }

        JsonObject recapture = new JsonObject();
        if (ConfigUtil.properties.isRecapture()) {
            recapture.addProperty("recapture", fraud != null ? StringUtils.jsonToBoolean(fraud.get("check_recapture").getAsJsonObject().get("recapture")) : null);
            recapture.addProperty("confidence", fraud != null ? StringUtils.jsonToDouble(fraud.get("check_recapture").getAsJsonObject().get("confidence")) : null);
        } else {
            recapture.addProperty("recapture", false);
            recapture.addProperty("confidence", 100.0);
        }

        fraudRes.add("check_edited", edit);
        fraudRes.add("check_recapture", recapture);

        String card_type = StringUtils.jsonToString(valueRes.get("card_type"));
        JsonObject confidence = new JsonObject();
        JsonObject value = new JsonObject();

        if (!"junk".equals(card_type)) {
            String valId = StringUtils.jsonToString(valueRes.get("id"));
            String valName = StringUtils.jsonToString(valueRes.get("name"));
            String valSex = StringUtils.jsonToString(valueRes.get("sex"));
            String valDob = StringUtils.jsonToString(valueRes.get("dob"));
            String valAddress = StringUtils.jsonToString(valueRes.get("province"));
            String valIssuedDate = StringUtils.jsonToString(valueRes.get("issued_date"));
            String valExpiredDate = StringUtils.jsonToString(valueRes.get("expired_date"));

            Double confidenceIdValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("id")), 0.0);
            Double confidenceNameValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("name")), 0.0);
            Double confidenceSexValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("sex")), 0.0);
            Double confidenceDobValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("dob")), 0.0);
            Double confidenceProvinceValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("province")), 0.0);
            Double confidenceIssueDateValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("issued_date")), 0.0);
            Double confidenceExpiredDateValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("expired_date")), 0.0);

            confidence.addProperty("id", confidenceIdValue);
            confidence.addProperty("name", confidenceNameValue);
            confidence.addProperty("sex", confidenceSexValue);
            confidence.addProperty("dob", confidenceDobValue);
            confidence.addProperty("province", confidenceProvinceValue);
            confidence.addProperty("issued_date", confidenceIssueDateValue);
            confidence.addProperty("expired_date", confidenceExpiredDateValue);

            if (ConfigUtil.properties.isSubstringIdNumber()
                    && !StringUtils.isNullEmpty(valId)
                    && valId.indexOf("(") > 0) {
                value.addProperty("id", valId.substring(0, valId.indexOf("(")).trim());
            } else {
                value.addProperty("id", valId);
            }
            value.addProperty("name", valName);
            TranslateService translateService = new TranslateService(reqTime);
            JsonObject sex = new JsonObject();
            String sexEn = TranslateService.getKeyMapping().get(valSex);
            if (StringUtils.isNullEmpty(sexEn)) {
                valSex = "ស្រី";
                sexEn = "Female";
            }
            sex.addProperty("kh", valSex);
            sex.addProperty("en", sexEn);
            value.add("sex", sex);

            JsonObject dob = new JsonObject();
            dob.addProperty("kh", valDob.replace(".", "/"));
            dob.addProperty("en", translateService.translateDob(valDob));
            value.add("dob", dob);

            JsonObject address = new JsonObject();
            address.addProperty("kh", valAddress);
            address.addProperty("en", translateService.toTranslate(valAddress));
            value.add("address", address);

            JsonObject province = new JsonObject();
            String provinceKh = utils.getProvince(valAddress);
            String provinceCode = null;
            if (!StringUtils.isNullEmpty(provinceKh)) {
                provinceCode = TranslateService.getKeyMapping().get(provinceKh);
            }
            province.addProperty("kh", provinceKh);
            province.addProperty("en", StringUtils.isNullEmpty(provinceCode) ? null : TranslateService.getKeyMapping().get(provinceCode));
            province.addProperty("code", provinceCode);
            value.add("province", province);

            JsonObject issueDate = new JsonObject();
            issueDate.addProperty("kh", valIssuedDate.replace(".", "/"));
            issueDate.addProperty("en", translateService.translateDob(valIssuedDate));
            value.add("issued_date", issueDate);

            JsonObject expiredDate = new JsonObject();
            expiredDate.addProperty("kh", valExpiredDate.replace(".", "/"));
            expiredDate.addProperty("en", translateService.translateDob(valExpiredDate));
            value.add("expired_date", expiredDate);

            value.addProperty("path_image", pathImage);
            value.add("fraud_detect", fraudRes);

            //todo: check quota
            SoapService soapService = new SoapService();
            soapRequest.setIdNo(StringUtils.jsonToString(value.get("id")));

            JsonObject response = soapService.postRequest(reqTime, soapRequest);

            if (response != null) {
                String errDesc = StringUtils.jsonToString(response.get("description"));
                Boolean canReg = StringUtils.jsonToBoolean(response.get("canRegister"), false);
                if (!canReg) {
                    //todo: return errCode + errMessage
                    responseDto = ResponseDto.builder()
                            .errCode(Error.code.NOT_ENOUGH_QUOTA)
                            .errMessage(errDesc)
                            .errMessageKh(errDesc)
                            .transId(transId)
                            .reqTime(reqTime)
                            .build();
                    log.info("reqTime: " + reqTime + " id transform finished " + (System.currentTimeMillis() - start) + "ms data: " + responseDto);
                    return responseDto;
                }
            }
            //todo: passed
            responseDto = ResponseDto.builder()
                    .errCode(Error.code.SUCCESS)
                    .errMessage(Error.message.SUCCESS)
                    .errMessageKh(Error.message_kh.SUCCESS)
                    .transId(transId)
                    .reqTime(reqTime)
                    .confidence(confidence)
                    .value(value)
                    .build();
        } else {
            //todo: image junk
            responseDto = ResponseDto.builder()
                    .errCode(Error.code.DOCUMENT_NOT_SUPPORT)
                    .errMessage(Error.message.DOCUMENT_NOT_SUPPORT)
                    .errMessageKh(Error.message_kh.DOCUMENT_NOT_SUPPORT)
                    .transId(transId)
                    .reqTime(reqTime)
                    .confidence(confidence)
                    .value(value)
                    .build();
        }

        log.info("reqTime: " + reqTime + " id transform finished " + (System.currentTimeMillis() - start) + "ms data: " + responseDto);
        return responseDto;
    }

    private ResponseDto passportProcess(long reqTime, String pathImage, String transId, SoapObjRequest soapRequest, JsonObject jsonResponse) {
        long start = System.currentTimeMillis();

        ResponseDto responseDto;
        JsonObject valueRes = jsonResponse.get("value").getAsJsonObject();
        String valDob = StringUtils.jsonToString(valueRes.get("birth_day"));
        String valCountry = StringUtils.jsonToString(valueRes.get("country"));
        String valDate_of_issue = StringUtils.jsonToString(valueRes.get("date_of_issue"));
        String valDocument_num = StringUtils.jsonToString(valueRes.get("document_num"));
        String valExpiry_date = StringUtils.jsonToString(valueRes.get("expiry_date"));
        String valGiven_name = StringUtils.jsonToString(valueRes.get("given_name"));
        String valPersonal_num = StringUtils.jsonToString(valueRes.get("personal_num"));
        String valPlace_of_birth = StringUtils.jsonToString(valueRes.get("place_of_birth"));
        String valSex = StringUtils.jsonToString(valueRes.get("sex"));
        String valSurname = StringUtils.jsonToString(valueRes.get("surname"));

        if (!"male".equalsIgnoreCase(valSex) && !"female".equalsIgnoreCase(valSex)) {
            valSex = "Female";
        }

        JsonObject value = new JsonObject();
        value.addProperty("birth_day", StringUtils.isNullEmpty(valDob) ? null : StringUtils.dateToStr(StringUtils.strToDate(valDob, "yyyy/MM/dd"), "dd/MM/yyyy"));
        value.addProperty("confidence", "");
        value.addProperty("country", valCountry);
        value.addProperty("date_of_issue", valDate_of_issue);
        value.addProperty("document_num", valDocument_num);
        value.addProperty("expiry_date", StringUtils.isNullEmpty(valExpiry_date) ? null : StringUtils.dateToStr(StringUtils.strToDate(valExpiry_date, "yyyy/MM/dd"), "dd/MM/yyyy"));
        value.addProperty("given_name", valGiven_name);
        value.addProperty("personal_num", valPersonal_num);
        value.addProperty("place_of_birth", valPlace_of_birth);
        value.addProperty("sex", valSex);
        value.addProperty("surname", valSurname);
        value.addProperty("path_image", pathImage);

        //todo: check quota
        SoapService soapService = new SoapService();
        soapRequest.setUrl(soapRequest.getUrl());
        soapRequest.setAccessKey(soapRequest.getAccessKey());
        soapRequest.setToken(soapRequest.getToken());
        soapRequest.setUserName(soapRequest.getUserName());
        soapRequest.setPassword(soapRequest.getPassword());
        soapRequest.setDealerIsdn(soapRequest.getDealerIsdn());
        soapRequest.setIdNo(StringUtils.jsonToString(value.get("id")));

        JsonObject response = soapService.postRequest(reqTime, soapRequest);

        if (response != null) {
            String errDesc = StringUtils.jsonToString(response.get("description"));
            Boolean canReg = StringUtils.jsonToBoolean(response.get("canRegister"));
            if (!canReg) {
                //todo: return errCode + errMessage
                responseDto = ResponseDto.builder()
                        .errCode(Error.code.NOT_ENOUGH_QUOTA)
                        .errMessage(errDesc)
                        .errMessageKh(errDesc)
                        .transId(transId)
                        .reqTime(reqTime)
                        .build();
                log.info("reqTime: " + reqTime + " passport transform finished " + (System.currentTimeMillis() - start) + "ms data: " + responseDto);
                return responseDto;
            }
        }
        //todo: passed
        responseDto = ResponseDto.builder()
                .errCode(Error.code.SUCCESS)
                .errMessage(Error.message.SUCCESS)
                .errMessageKh(Error.message_kh.SUCCESS)
                .transId(transId)
                .reqTime(reqTime)
                .value(value)
                .build();
        log.info("reqTime: " + reqTime + " passport transform finished " + (System.currentTimeMillis() - start) + "ms data: " + responseDto);
        return responseDto;
    }

}
