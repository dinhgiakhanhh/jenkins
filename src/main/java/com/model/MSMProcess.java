
package com.model;

import com.core.AppMain;
import com.entity.CdrLog;
import com.entity.SDPPartner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lib.ConfigUtil;
import com.lib.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author TT
 */
public class MSMProcess {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public SDPPartner getPartnerByToken(String transId, String token) {
        SDPPartner result = null;

        try {
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("cmd_code", "get_partner_info_by_token");
            jsonObj.addProperty("token", token);

            String uri = ConfigUtil.properties.getUrlAPIGW();
            String requestBody = jsonObj.toString();
            Utils util = new Utils();
            String output = util.postRequest(transId, uri, requestBody);
            if (!output.equals("")) {
                JsonObject jObj = new JsonParser().parse(output).getAsJsonObject();
                result = new SDPPartner();
                result.setId(Long.parseLong(util.jsonGetAsString(jObj.get("ID"))));
                result.setPartnerCode(util.jsonGetAsString(jObj.get("PARTNER_CODE")));
                result.setPartnerName(util.jsonGetAsString(jObj.get("PARTNER_NAME")));
                result.setToken(util.jsonGetAsString(jObj.get("TOKEN")));
                result.setIp(util.jsonGetAsString(jObj.get("IP")));
                result.setStatus(Integer.parseInt(util.jsonGetAsString(jObj.get("STATUS"))));
                result.setDescription(util.jsonGetAsString(jObj.get("DESCRIPTION")));
                result.setQuotaReal(Integer.parseInt(util.jsonGetAsString(jObj.get("QUOTA_REAL"))));
            }
        } catch (Exception ex) {
            log.error("Exception --- TransId: " + transId, ex);
        }
        return result;
    }

    public int minusQuota(String transId, long partnerId, int quota, String service_code) {
        try {
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("cmd_code", "minus_quota");
            jsonObj.addProperty("id", partnerId);
            jsonObj.addProperty("quota", quota);
            jsonObj.addProperty("service_code", service_code);

            String uri = ConfigUtil.properties.getUrlAPIGW();
            String requestBody = jsonObj.toString();

            String result = new Utils().postRequest(transId, uri, requestBody);
            int quotaRemain = Integer.parseInt(result);
            if (quotaRemain > 0) {
                return quotaRemain;
            } else {
                return 0;
            }
        } catch (Exception ex) {
            log.error("Exception --- TransId: " + transId, ex);
            return 0;
        }
    }

    public void insert_cdr_log(String transId, CdrLog cdr) {
        try {
            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("cmd_code", "insert_cdr_log");
            jsonObj.addProperty("partner_id", cdr.getPartnerId());
            jsonObj.addProperty("partner_name", cdr.getPartnerName());
            jsonObj.addProperty("service_code", cdr.getServiceCode());
            jsonObj.addProperty("request_time", cdr.getRequestTime());
            jsonObj.addProperty("response_time", cdr.getResponseTime());
            jsonObj.addProperty("req_ocr_time", cdr.getReqOcrTime());
            jsonObj.addProperty("res_ocr_time", cdr.getResOcrTime());
            jsonObj.addProperty("request_body", cdr.getRequestBody());
            jsonObj.addProperty("response_body", cdr.getResponseBody());
            jsonObj.addProperty("req_ocr_body", cdr.getReqOcrBody());
            jsonObj.addProperty("res_ocr_body", cdr.getResOcrBody());
            jsonObj.addProperty("result_code", cdr.getResultCode());
            jsonObj.addProperty("result_message", cdr.getResultMessage());
            jsonObj.addProperty("status", cdr.getStatus());
            jsonObj.addProperty("transid", transId);
            jsonObj.addProperty("quota_start", cdr.getQuotaStart());
            jsonObj.addProperty("card_type", cdr.getCardType());
            jsonObj.addProperty("side", cdr.getSide());
            jsonObj.addProperty("ip", cdr.getIp());

            String uri = ConfigUtil.properties.getUrlAPIGW();
            String requestBody = jsonObj.toString();

            new Utils().postRequest(transId, uri, requestBody);
        } catch (Exception ex) {
            log.error("Exception --- TransId: " + transId, ex);
        }
    }
}
