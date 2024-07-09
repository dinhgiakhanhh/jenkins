
package com.core;

import com.api.*;
import com.lib.ConfigUtil;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppMain {

    public static final String ApplicationName = "api_gateway_metfone";
    public static final String sourceLog = "log";
    public static List<String> ipServerList;

    public static void main(String[] args) {
        System.out.println(ApplicationName + " hello jenkins");
        ConfigUtil.loadProperties();
        applicationStart(sourceLog);

        ipServerList = new ArrayList<>();
        String[] ipServers = ConfigUtil.properties.getIpServer().split(";");
        int id = 0;
        for (String ip : ipServers) {
            id = id + 1;
            ipServerList.add(ip);
        }

        QueuedThreadPool threadPool = new QueuedThreadPool(100000, 100);
        Server server = new Server(threadPool);

        // HTTP connector
        ServerConnector httpServerConnector = new ServerConnector(server);
        httpServerConnector.setHost(ConfigUtil.properties.getHost());
        httpServerConnector.setPort(Integer.parseInt(ConfigUtil.properties.getPort()));
        httpServerConnector.setIdleTimeout(ConfigUtil.properties.getTimeOut() * 4 * 1000);
        server.addConnector(httpServerConnector);

        ContextHandler ocrHandler = new ContextHandler();
        ocrHandler.setContextPath("/api/request_ocr_id");
        ocrHandler.setHandler(new OcrHandler("id"));

        ContextHandler ocrPassport = new ContextHandler();
        ocrPassport.setContextPath("/api/request_ocr_passport");
        ocrPassport.setHandler(new OcrHandler("passport"));

        ContextHandler faceHandler = new ContextHandler();
        faceHandler.setContextPath("/api/request_face_match");
        faceHandler.setHandler(new FaceMatchHandler());

        ContextHandler uploadHandler = new ContextHandler();
        uploadHandler.setContextPath("/api/request_upload");
        uploadHandler.setHandler(new UploadHandler());

        ContextHandler provinceHandler = new ContextHandler();
        provinceHandler.setContextPath("/api/request_province");
        provinceHandler.setHandler(new ProvinceHandler());

        ContextHandler ocrHandlerV2 = new ContextHandler();
        ocrHandlerV2.setContextPath("/api/request_ocr_id_v2");
        ocrHandlerV2.setHandler(new OcrHandlerV2("id"));

        ContextHandler ocrPassportV2 = new ContextHandler();
        ocrPassportV2.setContextPath("/api/request_ocr_passport_v2");
        ocrPassportV2.setHandler(new OcrHandlerV2("passport"));

        ContextHandler uploadHandlerV2 = new ContextHandler();
        uploadHandlerV2.setContextPath("/api/request_upload_v2");
        uploadHandlerV2.setHandler(new UploadHandlerV2());

        ContextHandler faceHandlerV2 = new ContextHandler();
        faceHandlerV2.setContextPath("/api/request_face_match_v2");
        faceHandlerV2.setHandler(new FaceMatchHandlerV2());

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.addHandler(ocrHandler);
        handlerCollection.addHandler(ocrPassport);
        handlerCollection.addHandler(faceHandler);
        handlerCollection.addHandler(uploadHandler);
        handlerCollection.addHandler(provinceHandler);
        handlerCollection.addHandler(ocrHandlerV2);
        handlerCollection.addHandler(ocrPassportV2);
        handlerCollection.addHandler(uploadHandlerV2);
        handlerCollection.addHandler(faceHandlerV2);
        // Set a handler
        server.setHandler(handlerCollection);
        // Start the server
        try {
            server.start();
            server.join();
        } catch (Exception exception) {
            LoggerFactory.getLogger(AppMain.class).error("", exception);
        }

    }

    public static void applicationStart(String sourceLog) {
        File f;
        try {
            f = new File(sourceLog);
            if (!f.exists() || !f.isDirectory()) {
                boolean a = f.mkdirs();
                System.out.println(a);
            }

            f = new File("log4j.properties");
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    System.out.println();
                }
                try (PrintWriter writer = new PrintWriter("log4j.properties", "UTF-8")) {
                    writer.println("log4j.rootLogger=INFO, FILE");
                    writer.println("log4j.appender.FILE=org.apache.log4j.DailyRollingFileAppender");
                    writer.println("log4j.appender.FILE.File=" + sourceLog + "/" + ApplicationName + ".log");
                    writer.println("log4j.appender.FILE.DatePattern='.'yyyy-MM-dd-HH");
                    writer.println("log4j.appender.FILE.layout=org.apache.log4j.PatternLayout");
                    writer.println("log4j.appender.FILE.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
                }
            }
            PropertyConfigurator.configureAndWatch(f.getPath());
            f = new File("config.properties");
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    System.out.println();
                }
                try (PrintWriter writer = new PrintWriter("config.properties", "UTF-8")) {
                    writer.println("# Config Url");
                    writer.println("host=0.0.0.0");
                    writer.println("port=8889");
                    writer.println("timeout=30000");
                    writer.println("isSave=true");
                    writer.println("# Config url APIGW");
                    writer.println("urlAPIGW=http://172.16.206.51:8890/index.php/processRequest");
                    writer.println("# translate result true/false");
                    writer.println("is_translate=true");
                    writer.println("# key translate");
                    writer.println("translate_key=AIzaSyDnRwo6dg8y1_Ys5nTV9WQmWmTNCiy-d5Q");
                    writer.println("# Config url ocr base64");
                    writer.println("url_ocr_base64=http://172.16.106.56:30006/metfone/ocr/image_base64");
                    writer.println("# Config url face matching base 64");
                    writer.println("url_face_matching_base64=http://172.16.206.51:8081/face/v1/face_match/compare_images_minio");
                    writer.println("# Config url passport base64");
                    writer.println("url_passport_base64=http://172.16.106.56:8081/ocr_passport/image_base64");
                    writer.println("minus_quota=true");
                    writer.println("ip_server=103.1.210.36;103.1.210.37;172.16.108.11");
                    writer.println("url_translate=http://172.16.206.51:4567/translation/khm_2_eng");
                    writer.println("minio.access.key=minioadmin");
                    writer.println("minio.access.pass=minioadmin");
                    writer.println("minio.url=http://172.16.206.51:9000");
                    writer.println("minio.bucket=metfone");
                    writer.println("province.list=[{\"en\": \"Siem Reap\", \"kh\": \"សៀមរាប\", \"code\": \"SIE\"},{\"en\": \"Takeo\", \"kh\": \"តាកែវ\", \"code\": \"TAK\"},{\"en\": \"Prey Veng province\", \"kh\": \"ព្រៃវែង\", \"code\": \"PRE\"},{\"en\": \"Preah Vihear\", \"kh\": \"ព្រះវិហារ\", \"code\": \"PRH\"},{\"en\": \"Stung Treng\", \"kh\": \"ស្ទឹងត្រែង\", \"code\": \"STU\"},{\"en\": \"Kampong Thom\", \"kh\": \"កំពង់ធំ\", \"code\": \"THO\"},{\"en\": \"Kampot\", \"kh\": \"កំពត\", \"code\": \"KAM\"},{\"en\": \"Ratanak Kiri\", \"kh\": \"រតនគីរី\", \"code\": \"ROT\"},{\"en\": \"Svay Rieng\", \"kh\": \"ស្វាយរៀង\", \"code\": \"SVA\"},{\"en\": \"Krong Preah Sihanouk\", \"kh\": \"ព្រះសីហនុ\", \"code\": \"SIH\"},{\"en\": \"Pursat\", \"kh\": \"ពោធិ៍សាត់\", \"code\": \"PUR\"},{\"en\": \"Kampong Speu\", \"kh\": \"កំពង់ស្ពឺ\", \"code\": \"SPE\"},{\"en\": \"Krong Kep\", \"kh\": \"កែប\", \"code\": \"KEB\"},{\"en\": \"Koh Kong\", \"kh\": \"កោះកុង\", \"code\": \"KOH\"},{\"en\": \"Kampong Chhnang\", \"kh\": \"កំពង់ឆ្នាំង\", \"code\": \"CHH\"},{\"en\": \"Kampong Cham\", \"kh\": \"កំពង់ចាម\", \"code\": \"CHA\"},{\"en\": \"Phnom Penh City\", \"kh\": \"ភ្នំពេញ\", \"code\": \"PNP\"},{\"en\": \"Banteay Meanchey\", \"kh\": \"បន្ទាយមានជ័យ\", \"code\": \"BAN\"},{\"en\": \"Battambang\", \"kh\": \"បាត់ដំបង\", \"code\": \"BAT\"},{\"en\": \"Kandal\", \"kh\": \"កណ្តាល\", \"code\": \"KAN\"},{\"en\": \"Kracheh\", \"kh\": \"ក្រចេះ\", \"code\": \"KRA\"},{\"en\": \"Mondul Kiri\", \"kh\": \"មណ្ឌលគីរី\", \"code\": \"MON\"},{\"en\": \"Otdar Meanchey\", \"kh\": \"ឧត្តរមានជ័យ\", \"code\": \"ODD\"},{\"en\": \"Pailin City\", \"kh\": \"បៃលិន\", \"code\": \"PAI\"}]");
                    writer.println("substring.id.number=true");
                    writer.println("max.file.size=10");
                    writer.println("max.request.size=10");
                    writer.println("file.size.threshold=10");
                    writer.println("threshold.folder=/tmp");
                    writer.println("## ocr from minio");
                    writer.println("url.id.minio=http://172.16.106.58:6789/metfone/ocr/image_minio");
                    writer.println("url.passport.minio=http://172.16.106.58:6789/ocr_passport/image_minio");
                    writer.println("## config error code");
                    writer.println("err.not.passed=001,002,004,005,006");
                    writer.println("## require check fraud");
                    writer.println("fraud.check.edit=true");
                    writer.println("fraud.check.recapture=false");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AppMain.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
}
