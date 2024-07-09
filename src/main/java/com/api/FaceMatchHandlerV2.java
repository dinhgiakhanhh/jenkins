
package com.api;

import com.lib.Utils;
import com.service.FaceMatchServiceV2;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FaceMatchHandlerV2 extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    public FaceMatchHandlerV2() {}

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        long reqTime = System.currentTimeMillis();
        log.info("reqTime: " + reqTime + " Start call: " + request.getContextPath());

        request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, Utils.getMultipartConfig());

        FaceMatchServiceV2 faceMatchService = new FaceMatchServiceV2();
        String jsonString = faceMatchService.process(reqTime, baseRequest, request);

        response.setContentType("text/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        out.println(jsonString);
        baseRequest.setHandled(true);

        log.info("reqTime: " + reqTime + " End Call: " + request.getContextPath() + " -- " + (System.currentTimeMillis() - reqTime) + "ms");

    }
}
