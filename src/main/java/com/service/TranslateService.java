
package com.service;

import com.core.AppMain;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.translate.model.TranslationsResource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lib.ConfigUtil;
import com.lib.StringUtils;
import com.lib.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author KHANHDG
 * @since Sep 23, 2022
 */
public class TranslateService {

    private Translate translate;
    public final String code = "en";
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private long reqTime;
    private final Utils utils = new Utils();

    public long getReqTime() {
        return reqTime;
    }

    public void setReqTime(long reqTime) {
        this.reqTime = reqTime;
    }

    public Translate getTranslate() {
        return translate;
    }

    public void setTranslate(Translate translate) {
        this.translate = translate;
    }

    public TranslateService(long reqTime) {
        setReqTime(reqTime);
        setTranslate(new Translate.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), null).setApplicationName(AppMain.ApplicationName).build());
    }

    public String toTranslate(String str) {
        long start = System.currentTimeMillis();
        log.info("reqTime: " + getReqTime() + " start translate: " + str);
        String result = null;
        try {
            //todo: call api AI translate
            String url = ConfigUtil.properties.getUrlTranslate();
            boolean isConnect = this.utils.checkConnect(getReqTime() + "", url, 1);
            if (isConnect) {
                String body = "{\"khmer\": \"" + str + "\"}";
                String resBody = this.utils.postRequest(getReqTime() + "", url, body);
                if (!StringUtils.isNullEmpty(resBody)) {
                    JsonObject jo = new JsonParser().parse(resBody).getAsJsonObject();
                    result = jo.get("eng").getAsString();
                    if ("null".equalsIgnoreCase(result)) {
                        result = null;
                    }
                    log.info("reqTime: " + getReqTime() + " translate by model: " + result);
                }
            }
        } catch (Exception ex) {
            log.error("reqTime: " + getReqTime(), ex);
        }

        if (StringUtils.isNullEmpty(result)) {
            try {
                Translate.Translations.List list = getTranslate().new Translations().list(Collections.singletonList(str), code);
                list.setKey(ConfigUtil.properties.getKeyTranslate());
                TranslationsListResponse response = list.execute();
                for (TranslationsResource tr : response.getTranslations()) {
                    result = tr.getTranslatedText();
                }
                log.info("reqTime: " + getReqTime() + " translate by google: " + result);
            } catch (IOException e) {
                log.error("reqTime: " + getReqTime(), e);
            }
        }

        log.info("reqTime: " + getReqTime() + " finish translate " + str + " time: " + (System.currentTimeMillis() - start) + "ms");
        return result;
    }

    public String translateDob(String str) {
        String result = null;
        try {
            StringBuilder strBuilder = new StringBuilder();
            Map<String, String> map = TranslateService.getKeyMapping();
            for (char c : str.toCharArray()) {
                if (c == '.') {
                    strBuilder.append(c);
                } else {
                    String value = map.get(String.valueOf(c));
                    if (StringUtils.isNullEmpty(value)) {
                        value = toTranslate(String.valueOf(c));
                    }
                    strBuilder.append(value);
                }
            }
            if (!StringUtils.isNullEmpty(strBuilder.toString())) {
                result = strBuilder.toString().replace(".", "/");
            }
        } catch (Exception ex) {
            log.error("reqTime: " + getReqTime(), ex);
        }

        return result;
    }

    private static final Map<String, String> list = new HashMap<>();

    public static Map<String, String> getKeyMapping() {
        if (list.isEmpty()) {
            //todo: gender
            list.put("បុរស", "Male");
            list.put("ប្រុស", "Male");
            list.put("ច្រុស", "Male");
            list.put("ស្រី", "Female");

            //todo: number
            list.put("០", "0");
            list.put("១", "1");
            list.put("២", "2");
            list.put("៣", "3");
            list.put("៤", "4");
            list.put("៥", "5");
            list.put("៦", "6");
            list.put("៧", "7");
            list.put("៨", "8");
            list.put("៩", "9");

            //todo: province
            String provinceStr = ConfigUtil.properties.getProvinceList();
            if (!StringUtils.isNullEmpty(provinceStr)) {
                try {
                    JsonArray array = new JsonParser().parse(provinceStr).getAsJsonArray();
                    JsonObject jo;
                    for (int i = 0; i < array.size(); i++) {
                        jo = array.get(i).getAsJsonObject();
                        list.put(StringUtils.jsonToString(jo.get("kh")), StringUtils.jsonToString(jo.get("code")));
                        list.put(StringUtils.jsonToString(jo.get("code")), StringUtils.jsonToString(jo.get("en")));
                    }
                } catch (Exception ex) {
                    LoggerFactory.getLogger(TranslateService.class).error("getKeyMapping:", ex);
                }
            }
        }

        return list;
    }

}
