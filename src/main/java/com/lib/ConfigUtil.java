package com.lib;


import com.entity.PropertiesConfig;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Data
public class ConfigUtil {
    public static PropertiesConfig properties;

    private static String getProperty(Properties prop, String key) {
        return prop == null || prop.isEmpty() ? null : prop.getProperty(key);
    }

    public static void loadProperties() {
        long start = System.currentTimeMillis();
        Logger log = LoggerFactory.getLogger(ConfigUtil.class);
        log.info("---------Start load properties");
        if (properties == null) {
            properties = new PropertiesConfig();
            Properties prop = new Properties();
            try (FileInputStream fileInputSteam = new FileInputStream("config.properties");
                 InputStreamReader inputStreamReader = new InputStreamReader(fileInputSteam, StandardCharsets.UTF_8);
                 BufferedReader inputStream = new BufferedReader(inputStreamReader)) {
                prop.load(inputStream);
            } catch (Exception ex) {
                log.error("", ex);
            }

            properties.setTimeOut(Utils.getLongValue(getProperty(prop,"timeout"), 30));
            properties.setHost(getProperty(prop,"host"));
            properties.setPort(getProperty(prop,"port"));
            properties.setSave(Utils.getBooleanValue(getProperty(prop,"isSave"), false));
            properties.setUrlAPIGW(getProperty(prop,"urlAPIGW"));
            properties.setTranslate(Utils.getBooleanValue(getProperty(prop,"is_translate"), false));
            properties.setKeyTranslate(getProperty(prop,"translate_key"));
            properties.setUrlOcrBase64(getProperty(prop,"url_ocr_base64"));
            properties.setUrlFaceMatch(getProperty(prop,"url_face_matching_base64"));
            properties.setUrlPassportBase64(getProperty(prop,"url_passport_base64"));
            properties.setMinusQuota(Utils.getBooleanValue(getProperty(prop,"minus_quota"), true));
            properties.setIpServer(getProperty(prop,"ip_server"));
            properties.setUrlTranslate(getProperty(prop,"url_translate"));
            properties.setMinIOKey(getProperty(prop,"minio.access.key"));
            properties.setMinIOPass(getProperty(prop,"minio.access.pass"));
            properties.setMinIOUrl(getProperty(prop,"minio.url"));
            properties.setMinIOBucket(getProperty(prop,"minio.bucket"));
            properties.setProvinceList(getProperty(prop,"province.list"));
            properties.setServiceId(getProperty(prop,"service.id"));
            properties.setServicePassport(getProperty(prop,"service.passport"));
            properties.setServiceFaceCompare(getProperty(prop,"service.face.compare"));
            properties.setSubstringIdNumber(Utils.getBooleanValue(getProperty(prop,"substring.id.number"), true));
            properties.setMaxFileSize(Utils.getLongValue(getProperty(prop,"max.file.size"), 10));
            properties.setMaxRequestSize(Utils.getLongValue(getProperty(prop,"max.request.size"), 10));
            properties.setFileSizeThreshold(Utils.getIntValue(getProperty(prop,"file.size.threshold"), 10));
            properties.setThresholdFolder(getProperty(prop,"threshold.folder"));
            properties.setUrlIdMinio(getProperty(prop,"url.id.minio"));
            properties.setUrlPassportMinio(getProperty(prop,"url.passport.minio"));
            properties.setErrNotPassed(getProperty(prop,"err.not.passed"));
            properties.setEditable(Utils.getBooleanValue(getProperty(prop,"fraud.check.edit"), true));
            properties.setRecapture(Utils.getBooleanValue(getProperty(prop,"fraud.check.recapture"), false));
        }
        log.info("---------End load properties: " + (System.currentTimeMillis() - start) + "ms");
        printConfig(properties, log);
    }

    private static void printConfig(PropertiesConfig propertiesConfig, Logger log) {
        if (propertiesConfig != null) {
            log.info("##### Config url #####");
            log.info("Host : " + propertiesConfig.getHost());
            log.info("Port : " + propertiesConfig.getPort());
            log.info("Timeout : " + propertiesConfig.getTimeOut());
            log.info("##### Config Url #####");
            log.info("urlAPIGW : " + propertiesConfig.getUrlAPIGW());
            log.info("url_ocr_base64: : " + propertiesConfig.getUrlOcrBase64());
            log.info("url_face_matching_base64: : " + propertiesConfig.getUrlFaceMatch());
            log.info("url_passport_base64: : " + propertiesConfig.getUrlPassportBase64());
            log.info("translate : " + propertiesConfig.isTranslate());
            log.info("translate key : " + propertiesConfig.getKeyTranslate());
            log.info("ip_server : " + propertiesConfig.getIpServer());
            log.info("minus_quota : " + propertiesConfig.isMinusQuota());
            log.info("is_save : " + propertiesConfig.isSave());
            log.info("url_translate : " + propertiesConfig.getUrlTranslate());
            log.info("minio.access.key : " + propertiesConfig.getMinIOKey());
            log.info("minio.access.pass : " + propertiesConfig.getMinIOPass());
            log.info("minio.url : " + propertiesConfig.getMinIOUrl());
            log.info("minio.bucket : " + propertiesConfig.getMinIOBucket());
            log.info("province.list : " + propertiesConfig.getProvinceList());
            log.info("service.id : " + propertiesConfig.getServiceId());
            log.info("service.passport : " + propertiesConfig.getServicePassport());
            log.info("service.face.compare : " + propertiesConfig.getServiceFaceCompare());
            log.info("substring.id.number : " + propertiesConfig.isSubstringIdNumber());
            log.info("max.file.size : " + propertiesConfig.getMaxFileSize());
            log.info("max.request.size : " + propertiesConfig.getMaxRequestSize());
            log.info("file.size.threshold : " + propertiesConfig.getFileSizeThreshold());
            log.info("threshold.folder : " + propertiesConfig.getThresholdFolder());
            log.info("url.id.minio : " + propertiesConfig.getUrlIdMinio());
            log.info("url.passport.minio : " + propertiesConfig.getUrlPassportMinio());
            log.info("err.not.passed : " + propertiesConfig.getErrNotPassed());
            log.info("fraud.check.edit : " + propertiesConfig.isEditable());
            log.info("fraud.check.recapture : " + propertiesConfig.isRecapture());
        }
    }
}
