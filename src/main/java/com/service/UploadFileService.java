
package com.service;

import com.entity.ResponseDto;
import com.google.gson.JsonObject;
import com.lib.Error;
import com.lib.StringUtils;
import com.lib.Utils;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


public class UploadFileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final MinIOService minIOService;
    private final Utils util;

    public UploadFileService() {
        minIOService = new MinIOService();
        util = new Utils();
    }

    public String uploadProcess(long reqTime, Request baseRequest, HttpServletRequest request) {
        String transId = null;
        String pathImage = null;
        String result;
        String token = baseRequest.getHeader("token");
        String method = request.getMethod().toUpperCase();
        CommonService commonService = new CommonService();

        log.info("reqTime: " + reqTime + " -- Start Accept request Get Link: " + request.getContextPath() + ", Method: " + method + ", Token: " + token);

        if (!"POST".equals(method)) {
            return commonService.buildResponse(ResponseDto.builder()
                    .reqTime(reqTime)
                    .errCode(Error.code.METHOD_WRONG)
                    .errMessage(Error.message.METHOD_WRONG)
                    .errMessageKh(Error.message_kh.METHOD_WRONG)
                    .build());
        } else {
            InputStream inputStream = null;
            try {
                Collection<Part> parts = request.getParts();
                String fileName = null;
                for (Part part : parts) {
                    if (util.isFilePart(part)) {
                        fileName = util.extractFileName(part);
                        inputStream = part.getInputStream();
                    } else {
                        transId = request.getParameter("transid");
                        pathImage = request.getParameter("path_image");
                    }
                }
                if (!StringUtils.isNullEmpty(fileName)
                        && inputStream != null
                        && !StringUtils.isNullEmpty(transId)) {

                    String transIdFront = minIOService.getTransId(pathImage, transId);
                    result = minIOService.upload(inputStream, transIdFront, reqTime, "back");
                    if (!StringUtils.isNullEmpty(result)) {
                        JsonObject response = new JsonObject();
                        response.addProperty("path_image", result);
                        return commonService.buildResponse(ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.SUCCESS)
                                .errMessage(Error.message.SUCCESS)
                                .errMessageKh(Error.message_kh.SUCCESS)
                                .value(response)
                                .build());
                    } else {
                        return commonService.buildResponse(ResponseDto.builder()
                                .transId(transId)
                                .reqTime(reqTime)
                                .errCode(Error.code.CANNOT_UPLOAD_IMAGE_MINIO)
                                .errMessage(Error.message.CANNOT_UPLOAD_IMAGE_MINIO)
                                .errMessageKh(Error.message_kh.CANNOT_UPLOAD_IMAGE_MINIO)
                                .build());
                    }
                } else {
                    return commonService.buildResponse(ResponseDto.builder()
                            .transId(transId)
                            .reqTime(reqTime)
                            .errCode(Error.code.INVALID_PARAM)
                            .errMessage(Error.message.INVALID_PARAM)
                            .errMessageKh(Error.message_kh.INVALID_PARAM)
                            .build());
                }
            } catch (IOException | ServletException e) {
                log.error("reqTime: " + reqTime, e);
                return commonService.buildResponse(ResponseDto.builder()
                        .transId(transId)
                        .reqTime(reqTime)
                        .errCode(Error.code.SYSTEM_BUSY)
                        .errMessage(Error.message.SYSTEM_BUSY)
                        .errMessageKh(Error.message_kh.SYSTEM_BUSY)
                        .build());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("reqTime: " + reqTime, e);
                    }
                }
            }
        }
    }
}
