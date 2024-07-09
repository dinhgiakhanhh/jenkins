
package com.service;

import com.entity.CdrLog;
import com.entity.SDPPartner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lib.*;
import com.lib.Error;
import com.model.MSMProcess;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author KHANHDG
 * @since Sep 9, 2022
 */
public class OcrService {

    protected Logger log;
    protected Utils utils;
    protected MSMProcess partnerService;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final MinIOService minIOService;

    public OcrService() {
        log = LoggerFactory.getLogger(this.getClass());
        partnerService = new MSMProcess();
        utils = new Utils();
        minIOService = new MinIOService();
    }

    public String process(long reqTime, Request baseRequest, HttpServletRequest request, String docType) {

        log.info("reqTime:" + reqTime + " -- Start call OcrService.processBase64");
        String transId = null;
        String image = null;
        String token = baseRequest.getHeader("token");
        String method = request.getMethod().toUpperCase();

        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method + ", Token: " + token + ", Doc_type: " + docType);
        CdrLog cdr = new CdrLog();
        try {
            String ipServer = utils.getIpServer(reqTime);
            log.info("reqTime: " + reqTime + " ipServer: " + ipServer);
            cdr.setIp(ipServer);
            cdr.setCardType("real");
            if (!"POST".equals(method)) {
                return buildBodyInsertLog(null, reqTime, cdr, Error.code.METHOD_WRONG, Error.message.METHOD_WRONG, null, null, null);
            } else {
                String bodyRequest = Utils.getBody(request, reqTime);

                if (StringUtils.isNullEmpty(bodyRequest)) {
                    return buildBodyInsertLog(null, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null, null, null);
                } else {
                    try {
                        long start = System.currentTimeMillis();
                        JsonObject jsonRequest = new JsonParser().parse(bodyRequest).getAsJsonObject();
                        log.info("reqTime: " + reqTime + " finish parse request body: " + (System.currentTimeMillis() - start) + "ms");

                        transId = StringUtils.jsonToString(jsonRequest.get("transid"));
                        image = StringUtils.jsonToString(jsonRequest.get("image"));
                    } catch (Exception e) {
                        log.error("reqTime: " + reqTime, e);
                    }
                    if (StringUtils.isNullEmpty(token)
                            || StringUtils.isNullEmpty(transId)
                            || StringUtils.isNullEmpty(image)) {
                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.INVALID_PARAM, Error.message.INVALID_PARAM, null, null, null);
                    } else {
                        String urlImg = null;

                        //todo: push to minio
                        if (ConfigUtil.properties.isSave()) {
                            urlImg = minIOService.upload(image, transId, reqTime);
                            if (StringUtils.isNullEmpty(urlImg)) {
                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.CANNOT_UPLOAD_IMAGE_MINIO, Error.message.CANNOT_UPLOAD_IMAGE_MINIO, null, null, null);
                            }
                        }


                        JsonObject reqBody = new JsonObject();
                        reqBody.addProperty("transid", transId);
                        reqBody.addProperty("image", urlImg);
                        cdr.setRequestBody(reqBody.toString());
                        cdr.setReqOcrBody(reqBody.toString());
                        reqBody.addProperty("image", image);
                        cdr.setReqOcrTime(dateFormat.format(new Date()));
                        String url = null;
                        switch (docType) {
                            case Constant.DOC_TYPE_ID:
                                cdr.setServiceCode(ConfigUtil.properties.getServiceId());
                                cdr.setSide("front");
                                url = ConfigUtil.properties.getUrlOcrBase64();
                                break;
                            case Constant.DOC_TYPE_PP:
                                cdr.setServiceCode(ConfigUtil.properties.getServicePassport());
                                url = ConfigUtil.properties.getUrlPassportBase64();
                                break;
                            default:
                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.DOCUMENT_NOT_SUPPORT, Error.message.DOCUMENT_NOT_SUPPORT, null, null, null);
                        }

                        SDPPartner partner = partnerService.getPartnerByToken(String.valueOf(reqTime), token);
                        log.info("reqTime: " + reqTime + " finish getPartnerByToken ");
                        if (partner == null) {
                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.TOKEN_INVALID, Error.message.TOKEN_INVALID, null, null, null);
                        } else {
                            cdr.setPartnerId(partner.getId());
                            cdr.setPartnerName(partner.getPartnerName());
                            if (partner.getStatus() != 1) {
                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.USER_DEACTIVATE, Error.message.USER_DEACTIVATE, null, null, null);
                            } else {
                                if (!StringUtils.isNullEmpty(url)) {

                                    String result = utils.postRequest(String.valueOf(reqTime), url, reqBody.toString());

                                    cdr.setResOcrTime(dateFormat.format(new Date()));
                                    if (!StringUtils.isNullEmpty(result)) {
                                        cdr.setResOcrBody(utils.replaceLogBase64(result, "thumb_base64"));
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
                                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null, null, null);
                                        }
                                        JsonObject confidence = new JsonObject();
                                        JsonObject value = new JsonObject();
                                        switch (docType) {
                                            case Constant.DOC_TYPE_ID:
                                                JsonObject confidenceRes = jsonResponse.get("confidence").getAsJsonObject();
                                                JsonObject valueRes = jsonResponse.get("value").getAsJsonObject();
                                                JsonObject fraud = jsonResponse.get("fraud_detect") != null && !jsonResponse.get("fraud_detect").isJsonNull() ? jsonResponse.get("fraud_detect").getAsJsonObject() : null;
                                                JsonObject fraudRes = new JsonObject();
                                                JsonObject edit = new JsonObject();
                                                JsonObject recapture = new JsonObject();
                                                edit.addProperty("check_edited", fraud != null ? StringUtils.jsonToBoolean(fraud.get("check_edited").getAsJsonObject().get("check_edited")) : null);
                                                edit.addProperty("confidence", fraud != null ? StringUtils.jsonToDouble(fraud.get("check_edited").getAsJsonObject().get("confidence")) : null);
                                                recapture.addProperty("recapture", fraud != null ? StringUtils.jsonToBoolean(fraud.get("check_recapture").getAsJsonObject().get("recapture")) : null);
                                                recapture.addProperty("confidence", fraud != null ? StringUtils.jsonToDouble(fraud.get("check_recapture").getAsJsonObject().get("confidence")) : null);
                                                fraudRes.add("check_edited", edit);
                                                fraudRes.add("check_recapture", recapture);

                                                String card_type = StringUtils.jsonToString(valueRes.get("card_type"));
                                                if (!"junk".equals(card_type)) {
                                                    String valId = StringUtils.jsonToString(valueRes.get("id"));
                                                    String valName = StringUtils.jsonToString(valueRes.get("name"));
                                                    String valSex = StringUtils.jsonToString(valueRes.get("sex"));
                                                    String valDob = StringUtils.jsonToString(valueRes.get("dob"));
                                                    String valAddress = StringUtils.jsonToString(valueRes.get("province"));
                                                    String valIssuedDate = StringUtils.jsonToString(valueRes.get("issued_date"));
                                                    String valExpiredDate = StringUtils.jsonToString(valueRes.get("expired_date"));

                                                    confidence.addProperty("id", StringUtils.jsonToString(confidenceRes.get("id")));
                                                    confidence.addProperty("name", StringUtils.jsonToString(confidenceRes.get("name")));
                                                    confidence.addProperty("sex", StringUtils.jsonToString(confidenceRes.get("sex")));
                                                    confidence.addProperty("dob", StringUtils.jsonToString(confidenceRes.get("dob")));
                                                    confidence.addProperty("province", StringUtils.jsonToString(confidenceRes.get("province")));
                                                    confidence.addProperty("issued_date", StringUtils.jsonToString(confidenceRes.get("issued_date")));
                                                    confidence.addProperty("expired_date", StringUtils.jsonToString(confidenceRes.get("expired_date")));

                                                    Double confidenceIdValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("id")), 0.0);
                                                    Double confidenceNameValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("name")), 0.0);
                                                    Double confidenceSexValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("sex")), 0.0);
                                                    Double confidenceDobValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("dob")), 0.0);
                                                    Double confidenceProvinceValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("province")), 0.0);
                                                    Double confidenceIssueDateValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("issued_date")), 0.0);
                                                    Double confidenceExpiredDateValue = utils.toDouble(StringUtils.jsonToString(confidenceRes.get("expired_date")), 0.0);

                                                    if (confidenceIdValue < 0.75) {
                                                        valId = "";
                                                    }
                                                    if (confidenceNameValue < 0.75) {
                                                        valName = "";
                                                    }
                                                    if (confidenceSexValue < 0.75) {
                                                        valSex = "";
                                                    }
                                                    if (confidenceDobValue < 0.75) {
                                                        valDob = "";
                                                    }
                                                    if (confidenceProvinceValue < 0.75) {
                                                        valAddress = "";
                                                    }
                                                    if (confidenceIssueDateValue < 0.75) {
                                                        valIssuedDate = "";
                                                    }
                                                    if (confidenceExpiredDateValue < 0.75) {
                                                        valExpiredDate = "";
                                                    }

                                                    if (!valId.matches("[\\s()0-9]+")) {
                                                        valId = "";
                                                    }

                                                    if (!valDob.matches("[.០១២៣៤៥៦៧៨៩]+")) {
                                                        valDob = "";
                                                    }
                                                    if (valDob.length() != 10) {
                                                        valDob = "";
                                                    }

                                                    if (!valIssuedDate.matches("[.០១២៣៤៥៦៧៨៩]+")) {
                                                        valIssuedDate = "";
                                                    }
                                                    if (valIssuedDate.length() != 10) {
                                                        valIssuedDate = "";
                                                    }

                                                    if (!valExpiredDate.matches("[.០១២៣៤៥៦៧៨៩]+")) {
                                                        valExpiredDate = "";
                                                    }
                                                    if (valExpiredDate.length() != 10) {
                                                        valExpiredDate = "";
                                                    }
                                                    if(ConfigUtil.properties.isSubstringIdNumber()
                                                            && !StringUtils.isNullEmpty(valId)
                                                            && valId.indexOf("(") > 0){
                                                        value.addProperty("id", valId.substring(0, valId.indexOf("(")).trim());
                                                    }else{
                                                        value.addProperty("id", valId);
                                                    }
                                                    value.addProperty("name", valName);
                                                    TranslateService translateService = new TranslateService(reqTime);
                                                    JsonObject sex = new JsonObject();
                                                    String sexEn = TranslateService.getKeyMapping().get(valSex);
                                                    if(StringUtils.isNullEmpty(sexEn)){
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

                                                    value.addProperty("path_image", urlImg);
                                                    value.add("fraud_detect", fraudRes);


                                                    if (StringUtils.isNullEmpty(valId)) {
                                                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.RESULT_OCR_FIELD_NOT_ENOUGH, Error.message.RESULT_OCR_FIELD_NOT_ENOUGH, value, confidence, docType);
                                                    } else {
                                                        //todo: minus quota
                                                        boolean isMinusQuota = ConfigUtil.properties.isMinusQuota();
                                                        if (isMinusQuota) {
                                                            int quotaRemain = partnerService.minusQuota(transId, partner.getId(), 1, ConfigUtil.properties.getServiceId());
                                                            if (quotaRemain > 0) {
                                                                cdr.setQuotaStart(quotaRemain);
                                                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, value, confidence, docType);
                                                            } else {
                                                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.NOT_ENOUGH_QUOTA, Error.message.NOT_ENOUGH_QUOTA, value, confidence, docType);
                                                            }
                                                        } else {
                                                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, value, confidence, docType);
                                                        }
                                                    }
                                                } else {
                                                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.DOCUMENT_NOT_SUPPORT, Error.message.DOCUMENT_NOT_SUPPORT, value, confidence, docType);
                                                }
                                            case Constant.DOC_TYPE_PP:
                                                valueRes = jsonResponse.get("value").getAsJsonObject();
                                                String valDob = StringUtils.jsonToString(valueRes.get("birth_day"));
                                                //String valConfidence = StringUtils.jsonToString(valueRes.get("confidence"));
                                                String valCountry = StringUtils.jsonToString(valueRes.get("country"));
                                                String valDate_of_issue = StringUtils.jsonToString(valueRes.get("date_of_issue"));
                                                String valDocument_num = StringUtils.jsonToString(valueRes.get("document_num"));
                                                String valExpiry_date = StringUtils.jsonToString(valueRes.get("expiry_date"));
                                                String valGiven_name = StringUtils.jsonToString(valueRes.get("given_name"));
                                                String valPersonal_num = StringUtils.jsonToString(valueRes.get("personal_num"));
                                                String valPlace_of_birth = StringUtils.jsonToString(valueRes.get("place_of_birth"));
                                                String valSex = StringUtils.jsonToString(valueRes.get("sex"));
                                                String valSurname = StringUtils.jsonToString(valueRes.get("surname"));

                                                if(!"male".equalsIgnoreCase(valSex) && !"female".equalsIgnoreCase(valSex)){
                                                    valSex = "Female";
                                                }

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
                                                value.addProperty("path_image", urlImg);


                                                if (StringUtils.isNullEmpty(valDocument_num)) {
                                                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.RESULT_OCR_FIELD_NOT_ENOUGH, Error.message.RESULT_OCR_FIELD_NOT_ENOUGH, value, confidence, docType);
                                                } else {
                                                    if (StringUtils.isNullEmpty(valGiven_name) && StringUtils.isNullEmpty(valSurname)) {
                                                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.DOCUMENT_NOT_SUPPORT, Error.message.DOCUMENT_NOT_SUPPORT, null, null, null);
                                                    } else {
                                                        if (valGiven_name.matches(".*\\d.*") || valSurname.matches(".*\\d.*")) {
                                                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.DOCUMENT_NOT_SUPPORT, Error.message.DOCUMENT_NOT_SUPPORT, null, null, null);
                                                        } else {
                                                            //todo: minus quota in face-match
                                                            boolean isMinusQuota = ConfigUtil.properties.isMinusQuota();
                                                            if (isMinusQuota) {
                                                                int quotaRemain = partnerService.minusQuota(transId, partner.getId(), 1, ConfigUtil.properties.getServicePassport());
                                                                if (quotaRemain > 0) {
                                                                    cdr.setQuotaStart(quotaRemain);
                                                                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.DOCUMENT_NOT_SUPPORT, value, confidence, docType);
                                                                } else {
                                                                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.NOT_ENOUGH_QUOTA, Error.message.NOT_ENOUGH_QUOTA, value, confidence, docType);
                                                                }
                                                            }else {
                                                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, value, confidence, docType);
                                                            }
                                                        }
                                                    }
                                                }
                                            default:
                                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.DOCUMENT_NOT_SUPPORT, Error.message.DOCUMENT_NOT_SUPPORT, null, null, null);
                                        }
                                    } else {
                                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null, null, null);
                                    }
                                } else {
                                    return buildBodyInsertLog(transId, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null, null, null);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("reqTime: " + reqTime, ex);
            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SYSTEM_BUSY, Error.message.SYSTEM_BUSY, null, null, null);
        }

    }


    public String uploadProcess(long reqTime, Request baseRequest, HttpServletRequest request) {

        log.info("reqTime:" + reqTime + " -- Start call OcrService.uploadProcess");
        String transId = null;
        String image = null;
        String pathImage = null;
        String token = baseRequest.getHeader("token");
        String method = request.getMethod().toUpperCase();

        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method + ", Token: " + token);
        CdrLog cdr = new CdrLog();
        try {
            String ipServer = utils.getIpServer(reqTime);
            log.info("reqTime: " + reqTime + " ipServer: " + ipServer);
            cdr.setIp(ipServer);
            cdr.setCardType("real");
            if (!"POST".equals(method)) {
                return buildBodyInsertLog(null, reqTime, cdr, Error.code.METHOD_WRONG, Error.message.METHOD_WRONG, null, null, null);
            } else {
                String bodyRequest = Utils.getBody(request, reqTime);

                if (StringUtils.isNullEmpty(bodyRequest)) {
                    return buildBodyInsertLog(null, reqTime, cdr, Error.code.OCR_FAIL, Error.message.OCR_FAIL, null, null, null);
                } else {
                    try {
                        JsonObject jsonRequest = new JsonParser().parse(bodyRequest).getAsJsonObject();
                        log.info("reqTime: " + reqTime + " finish parse request body");

                        transId = StringUtils.jsonToString(jsonRequest.get("transid"));
                        image = StringUtils.jsonToString(jsonRequest.get("image"));
                        pathImage = StringUtils.jsonToString(jsonRequest.get("path_image"));
                    } catch (Exception e) {
                        log.error("reqTime: " + reqTime, e);
                    }
                    if (StringUtils.isNullEmpty(token)
                            || StringUtils.isNullEmpty(transId)
                            || StringUtils.isNullEmpty(image)
                            || StringUtils.isNullEmpty(pathImage)) {
                        return buildBodyInsertLog(transId, reqTime, cdr, Error.code.INVALID_PARAM, Error.message.INVALID_PARAM, null, null, null);
                    } else {
                        String urlImg = null;

                        //todo: push to minio
                        if (ConfigUtil.properties.isSave()) {
                            String transIdFront = minIOService.getTransId(pathImage, transId);
                            urlImg = minIOService.upload(image, transIdFront, reqTime);
                            if (StringUtils.isNullEmpty(urlImg)) {
                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.CANNOT_UPLOAD_IMAGE_MINIO, Error.message.CANNOT_UPLOAD_IMAGE_MINIO, null, null, null);
                            }
                        }

                        SDPPartner partner = partnerService.getPartnerByToken(String.valueOf(reqTime), token);
                        log.info("reqTime: " + reqTime + " finish getPartnerByToken ");
                        if (partner == null) {
                            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.TOKEN_INVALID, Error.message.TOKEN_INVALID, null, null, null);
                        } else {
                            cdr.setPartnerId(partner.getId());
                            cdr.setPartnerName(partner.getPartnerName());
                            if (partner.getStatus() != 1) {
                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.USER_DEACTIVATE, Error.message.USER_DEACTIVATE, null, null, null);
                            } else {
                                JsonObject reqBody = new JsonObject();
                                reqBody.addProperty("transid", transId);
                                reqBody.addProperty("path_image", pathImage);
                                reqBody.addProperty("image", urlImg);
                                cdr.setRequestBody(reqBody.toString());
                                cdr.setSide("back");
                                JsonObject value = new JsonObject();
                                value.addProperty("path_image", urlImg);
                                return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SUCCESS, Error.message.SUCCESS, value, null, null);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("reqTime: " + reqTime, ex);
            return buildBodyInsertLog(transId, reqTime, cdr, Error.code.SYSTEM_BUSY, Error.message.SYSTEM_BUSY, null, null, null);
        }

    }

    public String buildBodyInsertLog(String transId, long reqTime, CdrLog cdr, String errCode, String errMessage, JsonObject value, JsonObject confidence, String docType) {
        log.info("reqTime: " + reqTime + " build response");
        long resTime = System.currentTimeMillis();
        JsonObject response = new JsonObject();
        response.addProperty("code", errCode);
        response.addProperty("message", errMessage);
        response.addProperty("transid", transId);

        if (Constant.DOC_TYPE_ID.equals(docType)) {
            response.add("confidence", confidence);
        }

        response.add("value", value);
        cdr.setRequestTime(dateFormat.format(reqTime));
        cdr.setResponseTime(dateFormat.format(resTime));
        cdr.setResponseBody(response.toString());
        cdr.setResultMessage(errMessage);
        if (Error.code.SUCCESS.equals(errCode)) {
            cdr.setStatus(1);
            cdr.setResultCode(Error.code.SUCCESS);
        } else {
            cdr.setStatus(0);
            cdr.setResultCode(errCode);
        }

        //if (!cdr.getIP().equals("127.0.0.1") && !cdr.getIP().equals("0:0:0:0:0:0:0:1")) {
        partnerService.insert_cdr_log(transId, cdr);
        //}

        log.info("reqTime: " + reqTime + " response: " + response);
        log.info("reqTime: " + reqTime + " end build response ----- " + (resTime - reqTime) + "ms");

        return response.toString();
    }
}
