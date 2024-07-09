package com.entity;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDto {
    private String transId;
    private long reqTime;
    private String errCode;
    private String errMessage;
    private String errMessageKh;
    private JsonObject confidence;
    private JsonObject value;
}
