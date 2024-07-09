package com.entity;

import com.lib.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {

    private String gwStatus;
    private String gwMessage;
    private String gwBody;

    public String buildResponse() {
        return "{" +
                "\"gw_status\":\"" +
                (StringUtils.isNullEmpty(gwStatus) ? "null" : gwStatus) +
                "\"," +
                "\"gw_message\":\"" +
                (StringUtils.isNullEmpty(gwMessage) ? "null" : gwMessage) +
                "\"," +
                "\"gw_body\":" +
                (StringUtils.isNullEmpty(gwBody) ? "null" : gwBody) +
                "}";
    }

}
