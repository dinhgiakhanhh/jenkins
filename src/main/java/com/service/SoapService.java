package com.service;


import com.entity.SoapObjRequest;
import com.google.gson.JsonObject;
import com.lib.StringUtils;
import com.lib.Utils;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SoapService {

    private final Logger log =  LoggerFactory.getLogger(SoapService.class);
    private final MediaType mediaType = MediaType.parse("text/xml; charset=utf-8");

    public SoapService() {
    }

    public JsonObject postRequest(long reqTime, SoapObjRequest requestBody) {
        long start = System.currentTimeMillis();
        log.info("reqTime " + reqTime + " - Start call method POST uri:" + requestBody.getUrl());
        JsonObject jonRes = null;

        if(!StringUtils.isNullEmpty(requestBody.getUrl())
        && !StringUtils.isNullEmpty(requestBody.getAccessKey())
        && !StringUtils.isNullEmpty(requestBody.getDealerIsdn())
        && !StringUtils.isNullEmpty(requestBody.getPassword())
        && !StringUtils.isNullEmpty(requestBody.getIdNo())
        && !StringUtils.isNullEmpty(requestBody.getToken())
        && !StringUtils.isNullEmpty(requestBody.getUserName())){

            String reqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:web=\"http://webservice.bccsgw.viettel.com/\" > " +
                    "    <soapenv:Header/> " +
                    "    <soapenv:Body> " +
                    "        <web:gwOperation> " +
                    "            <Input> " +
                    "                <wscode>checkSubByIdNo</wscode> " +
                    "                <accessKey>" + requestBody.getAccessKey() + "</accessKey> " +
                    "                <username>" + requestBody.getUserName() + "</username> " +
                    "                <password>" + requestBody.getPassword() + "</password> " +
                    "                <param name=\"token\" value=\"" + requestBody.getToken() + "\"/> " +
                    "                <param name=\"dealerIsdn\" value=\"" + requestBody.getDealerIsdn() + "\"/> " +
                    "                <param name=\"idNumber\" value=\""+ requestBody.getIdNo() + "\"/> " +
                    "    </Input> " +
                    "</web:gwOperation> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";

            log.info("reqTime " + reqTime + " - request body soap:" + reqBody);

            RequestBody body = RequestBody.create(mediaType, reqBody);
            Request request = new Request.Builder()
                    .url(requestBody.getUrl())
                    .post(body)
                    .addHeader("Content-Type", "text/xml; charset=utf-8")
                    .build();
            Utils utils = new Utils();
            try (Response response = utils.getClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String result = Objects.requireNonNull(response.body()).string();
                    log.info("reqTime " + reqTime + " - response body soap:" + result);
                    if(!StringUtils.isNullEmpty(result)){
                        String prefix = "errorCode&gt;";
                        String suffix = "&lt;/errorCode";
                        String errorCode = result.substring(result.indexOf(prefix) + prefix.length(), result.lastIndexOf(suffix));

                        prefix = "errorDescription&gt;";
                        suffix = "&lt;/errorDescription";
                        String description = result.substring(result.indexOf(prefix) + prefix.length(), result.lastIndexOf(suffix));

                        prefix = "canRegister&gt;";
                        suffix = "&lt;/canRegister";
                        String canRegister = result.substring(result.indexOf(prefix) + prefix.length(), result.lastIndexOf(suffix));

                        jonRes = new JsonObject();
                        jonRes.addProperty("errorCode", errorCode);
                        jonRes.addProperty("description", description);
                        jonRes.addProperty("canRegister", canRegister);

                    }
                }
            } catch (Exception e) {
                log.error("SeqId " + reqTime, e);
            }
        }

        log.info("SeqId " + reqTime + " - End call method POST uri:" + requestBody.getUrl() + " Response: " + jonRes + "- " + (System.currentTimeMillis() - start) + " ms");
        return jonRes;
    }

}
