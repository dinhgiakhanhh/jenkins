package com.lib;

import com.core.AppMain;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Utils {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Utils.class);
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static OkHttpClient client;
    private static MultipartConfigElement multipartConfigElement;

    public Utils() {
    }

    public OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(ConfigUtil.properties.getTimeOut(), TimeUnit.SECONDS)
                    .readTimeout(ConfigUtil.properties.getTimeOut(), TimeUnit.SECONDS)
                    .writeTimeout(ConfigUtil.properties.getTimeOut(), TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public static String getBody(HttpServletRequest request, long reqTime) {
        long start = System.currentTimeMillis();
        StringBuilder body = null;
        try {
            body = new StringBuilder(IOUtils.toString(request.getReader()));
        } catch (IOException e) {
            log.error("reqTime: " + reqTime, e);
        }
        log.info("reqTime: " + reqTime + " finish get body request " + (System.currentTimeMillis() - start) + "ms");
        return body == null ? null : body.toString();
    }


    public String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }


    public String postRequest(String seqId, String uri, String requestBody) {
        long start = System.currentTimeMillis();
        //String reqLog = requestBody;
        //reqLog = replaceLogBase64(reqLog, "image");
        log.info("SeqId " + seqId + " - Start call method POST uri:" + uri + " - body: " + requestBody);
        String output = "";
        //String logOutput = "";
        try {
            RequestBody body = RequestBody.create(requestBody, JSON);

            Request request = new Request.Builder()
                    .url(uri)
                    .post(body)
                    .build();

            if (client == null) {
                client = getClient();
            }
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    output = Objects.requireNonNull(response.body()).string();
                    //logOutput = output;
                    //logOutput = replaceLogBase64(logOutput, "thumb_base64");
                }
            } catch (ProtocolException ex) {
                //
            } catch (Exception e) {
                log.error("SeqId " + seqId, e);
            }
        } catch (Exception e) {
            log.error("SeqId " + seqId, e);
        }

        log.info("SeqId " + seqId + " - End call method POST uri:" + uri + " Response: " + output + "- " + (System.currentTimeMillis() - start) + " ms");
        return output;
    }


    public String replaceLogBase64(String input, String token) {
        if (StringUtils.isNullEmpty(input)) {
            return null;
        }
        try {
            JsonElement jEl = new JsonParser().parse(input);
            JsonObject jObj = null;
            if (jEl.isJsonObject()) {
                jObj = jEl.getAsJsonObject();
            }

            if (jEl.isJsonArray()) {
                if (jEl.getAsJsonArray().size() > 0) {
                    jObj = jEl.getAsJsonArray().get(0).getAsJsonObject();
                }
            }

            if (jObj != null && !StringUtils.isNullEmpty(StringUtils.jsonToString(jObj.get(token)))) {
                if (!jObj.get(token).isJsonNull()) {
                    jObj.remove(token);
                    jObj.addProperty(token, "Base64");
                    input = jObj.toString();
                }
            } else {
                if (input.contains(token)) {
                    String temp = input.substring(input.indexOf(token));
                    temp = temp.substring(0, temp.indexOf("\"}"));
                    input = input.replace(temp, token.replace("_", "") + "\":\"Base64");
                }
            }

        } catch (JsonSyntaxException e) {
            try {
                if (input.contains(token)) {
                    String temp = input.substring(input.indexOf(token));
                    temp = temp.substring(0, temp.indexOf("\"}"));
                    input = input.replace(temp, token.replace("_", "") + "\":\"Base64");
                }
            } catch (Exception ex) {
                log.error("", e);
            }
        }
        return input;
    }

    public String jsonGetAsString(JsonElement element) {
        String result = "";
        try {
            if (!element.isJsonNull()) {
                result = element.getAsString();
            }
        } catch (Exception e) {
            //log.error(Arrays.toString(e.getStackTrace()));
        }
        return result;
    }

    public static Long getLongValue(String value, long def) {
        long newValue = def;
        try {
            newValue = Long.parseLong(value);
        } catch (Exception ex) {
            //log.error(Arrays.toString(ex.getStackTrace()));
        }
        return newValue;
    }

    public static int getIntValue(String value, int def) {
        int newValue = def;
        try {
            newValue = Integer.parseInt(value);
        } catch (Exception ex) {
            //log.error(Arrays.toString(ex.getStackTrace()));
        }
        return newValue;
    }

    public static boolean getBooleanValue(String value, boolean def) {
        boolean newValue = def;
        try {
            newValue = Boolean.parseBoolean(value);
        } catch (Exception ex) {
            //log.error(Arrays.toString(ex.getStackTrace()));
        }
        return newValue;
    }


    public Double toDouble(String str, Double def) {
        try {
            return Double.parseDouble(str);
        } catch (Exception ex) {
            return def;
        }
    }

    public String getIpServer(long reqTime) {
        String ip = null;
        try {

            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    boolean isCheck = AppMain.ipServerList.stream().anyMatch(o -> o.equals(inetAddress.getHostAddress()));
                    if (isCheck) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("reqTime: " + reqTime + " Can not get IP server " + ex);
        }

        return ip;
    }

    public boolean checkConnect(String reqTime, String strUrl, int timeout) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setConnectTimeout(timeout * 1000);
            urlConn.connect();
            log.info("reqTime: " + reqTime + " --check connect url: " + strUrl + " -- response code: " + urlConn.getResponseCode());
            return !StringUtils.isNullEmpty(urlConn.getResponseCode() + "");
        } catch (IOException e) {
            log.error("reqTime: " + reqTime + " --Error creating HTTP connection: " + strUrl, e);
            return false;
        }
    }

    public Float getPercentMatchConfig(String transId, long partnerId) {
        Float percent = null;
        try {
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("cmd_code", "get_percent_config_active");
            jsonObj.addProperty("partner_id", partnerId);

            String uri = ConfigUtil.properties.getUrlAPIGW();
            String result = postRequest(transId, uri, jsonObj.toString());
            if (!StringUtils.isNullEmpty(result)) {
                JsonObject data = new JsonParser().parse(result).getAsJsonArray().get(0).getAsJsonObject();
                percent = StringUtils.jsonToFloat(data.get("PERCENT_MATCH"));
            }
        } catch (Exception ex) {
            log.error("Exception --- TransId: " + transId, ex);
        }

        return percent;
    }

    public String getProvince(String address) {
        String province = null;
        try {
            if (!StringUtils.isNullEmpty(address)) {
                province = address.substring(address.lastIndexOf(" ") + 1).trim();
            }
        } catch (Exception ex) {
            log.error("getProvince: ", ex);
        }
        return province;
    }

    public boolean isFilePart(Part part) {
        String contentType = part.getContentType();
        return contentType != null && !contentType.startsWith("text");
    }

    public String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return null;
    }

    public static MultipartConfigElement getMultipartConfig() {
        if (multipartConfigElement == null) {
            multipartConfigElement = new MultipartConfigElement(
                    ConfigUtil.properties.getThresholdFolder(),
                    1024 * 1024 * ConfigUtil.properties.getMaxFileSize(),
                    1024 * 1024 * ConfigUtil.properties.getMaxRequestSize(),
                    1024 * 1024 * ConfigUtil.properties.getFileSizeThreshold());
        }
        return multipartConfigElement;
    }
}
