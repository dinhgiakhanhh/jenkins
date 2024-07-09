
package com.api;

import com.lib.StringUtils;
import com.service.OcrService;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


public class UploadHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public UploadHandler() {
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        long reqTime = System.currentTimeMillis();
        log.info("reqTime: " + reqTime + " Start Call UploadHandler");

        OcrService ocrs = new OcrService();
        String jsonString = ocrs.uploadProcess(reqTime, baseRequest, request);

        int httpResponse;
        if (StringUtils.isNullEmpty(jsonString)) {
            httpResponse = HttpServletResponse.SC_NOT_FOUND;
        } else {
            httpResponse = HttpServletResponse.SC_OK;
        }
        response.setContentType("text/json; charset=utf-8");
        response.setStatus(httpResponse);
        PrintWriter out = response.getWriter();
        out.println(jsonString);
        baseRequest.setHandled(true);

        log.info("reqTime: " + reqTime + " httpResponse:" + httpResponse);
        log.info("reqTime: " + reqTime + " End Call UploadHandler");

    }

}
