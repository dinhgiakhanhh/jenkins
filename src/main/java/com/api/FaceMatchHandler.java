/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.api;

import com.lib.StringUtils;
import com.service.FaceMatchService;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author KHANHDG
 * @since Sep 12, 2022
 */
public class FaceMatchHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long reqTime = System.currentTimeMillis();
        log.info("reqTime: " + reqTime + " -------------------Start Call FaceMatchHandler");

        FaceMatchService faceMatchService = new FaceMatchService();
        String jsonString = faceMatchService.process(reqTime, baseRequest, request);

        int httpResponse;
        if (StringUtils.isNullEmpty(jsonString)) {
            httpResponse = HttpServletResponse.SC_NOT_FOUND;
        } else {
            httpResponse = HttpServletResponse.SC_OK;
        }        response.setContentType("text/json; charset=utf-8");
        response.setStatus(httpResponse);
        PrintWriter out = response.getWriter();
        out.println(jsonString);
        baseRequest.setHandled(true);

        log.info("reqTime: " + reqTime + " httpResponse:" + httpResponse);
        log.info("reqTime: " + reqTime + " ------------------End Call FaceMatchHandler: " + (System.currentTimeMillis() - reqTime) + "ms");
    }

}
