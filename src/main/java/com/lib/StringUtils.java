
package com.lib;

import com.google.gson.JsonElement;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author PHANNKVOS
 */
public class StringUtils {


    public static boolean isNullEmpty(String str) {
        try {
            return str.trim().isEmpty();
        } catch (Exception ex) {
            return true;
        }
    }

    public static String jsonToString(JsonElement element) {
        String result;
        try {
            result = element.getAsString();
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public static Boolean jsonToBoolean(JsonElement element) {
        Boolean result;
        try {
            result = element.getAsBoolean();
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public static Boolean jsonToBoolean(JsonElement element, boolean def) {
        boolean result;
        try {
            result = element.getAsBoolean();
        } catch (Exception e) {
            result = def;
        }
        return result;
    }

    public static Double jsonToDouble(JsonElement element) {
        Double result;
        try {
            result = element.getAsDouble();
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public static Float jsonToFloat(JsonElement element) {
        Float result;
        try {
            result = element.getAsFloat();
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public static Date strToDate(String value, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return  sdf.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String dateToStr(Date value, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.format(value);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
