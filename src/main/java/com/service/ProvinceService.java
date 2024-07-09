
package com.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lib.ConfigUtil;
import com.lib.Error;
import com.lib.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public class ProvinceService {

    protected Logger log;

    public ProvinceService() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public String getProvinceList(long reqTime, HttpServletRequest request) {
        log.info("reqTime:" + reqTime + " -- Start call ProvinceService.getProvinceList");
        String method = request.getMethod().toUpperCase();
        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method);

        String response = null;
        if ("GET".equals(method)) {
            try {
                JsonArray result = new JsonParser().parse(ConfigUtil.properties.getProvinceList()).getAsJsonArray();
                if(result != null && !result.isJsonNull() && result.size() > 0){
                    JsonObject jo = new JsonObject();
                    jo.addProperty("code", Error.code.SUCCESS);
                    jo.addProperty("message", Error.message.SUCCESS);
                    jo.addProperty("message_kh", Error.message_kh.SUCCESS);
                    jo.add("provinces", result);

                    response = jo.toString();
                }
            } catch (Exception ex) {
                log.error("reqTime: " + reqTime, ex);
            }
        }
        if(StringUtils.isNullEmpty(response)){
            JsonObject jo = new JsonObject();
            jo.addProperty("code", Error.code.NOT_FOUND_DATA);
            jo.addProperty("message", Error.message.NOT_FOUND_DATA);
            jo.addProperty("message_kh", Error.message_kh.NOT_FOUND_DATA);
            jo.add("provinces", null);
            response = jo.toString();
        }
        log.info("reqTime: " + reqTime + " -- province list: " + response);
        return response;
    }
}
