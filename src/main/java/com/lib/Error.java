package com.lib;

public interface Error {

    interface code{
        String SUCCESS = "000";
        String INVALID_PARAM = "001";
        String NOT_ENOUGH_QUOTA = "002";
        String SYSTEM_BUSY = "003";
        String METHOD_WRONG = "004";
        String TOKEN_INVALID = "005";
        String USER_DEACTIVATE = "006";
        String OCR_FAIL = "008";
        String FAKE = "009";
        String NOT_FOUND_DATA = "010";
        String RESULT_OCR_FIELD_NOT_ENOUGH = "011";
        String MISSING_DOCUMENT_IMAGE = "012";
        String DOCUMENT_NOT_SUPPORT = "013";
        String CANNOT_UPLOAD_IMAGE_MINIO = "014";
        String PERCENT_MATCH_NOT_CONFIG = "015";
    }

    interface message{
        String SUCCESS = "Success";
        String INVALID_PARAM = "Invalid params";
        String NOT_ENOUGH_QUOTA = "Not enough quota";
        String SYSTEM_BUSY = "System error";
        String METHOD_WRONG = "Method is not support";
        String TOKEN_INVALID = "Token invalid";
        String USER_DEACTIVATE = "User inactive";
        String OCR_FAIL = "OCR service failed";
        String FAKE = "Image was fake";
        String NOT_FOUND_DATA = "Not found";
        String RESULT_OCR_FIELD_NOT_ENOUGH = "Missing IDNO from document";
        String MISSING_DOCUMENT_IMAGE = "Missing document image";
        String DOCUMENT_NOT_SUPPORT = "Document is not support";
        String CANNOT_UPLOAD_IMAGE_MINIO = "Could not upload image to minio";
        String PERCENT_MATCH_NOT_CONFIG = "Percent match is not config";
    }

    interface message_kh{
        String SUCCESS = "ជោគជ័យ";
        String INVALID_PARAM = "ប៉ារ៉ាម៉ែត្រមិនត្រឹមត្រូវ";
        String NOT_ENOUGH_QUOTA = "មិនមានកូតាគ្រប់គ្រាន់ទេ។";
        String SYSTEM_BUSY = "កំហុសប្រព័ន្ធ";
        String METHOD_WRONG = "វិធីសាស្រ្តមិនត្រូវបានគាំទ្រទេ។";
        String TOKEN_INVALID = "និមិត្តសញ្ញាមិនត្រឹមត្រូវ";
        String USER_DEACTIVATE = "អ្នកប្រើប្រាស់អសកម្ម";
        String OCR_FAIL = "សេវាកម្ម OCR បានបរាជ័យ";
        String FAKE = "រូបភាពគឺក្លែងក្លាយ";
        String NOT_FOUND_DATA = "រក\u200Bមិន\u200Bឃើញ";
        String RESULT_OCR_FIELD_NOT_ENOUGH = "បាត់ IDNO ពីឯកសារ";
        String MISSING_DOCUMENT_IMAGE = "បាត់រូបភាពឯកសារ";
        String DOCUMENT_NOT_SUPPORT = "ឯកសារមិនគាំទ្រទេ។";
        String CANNOT_UPLOAD_IMAGE_MINIO = "មិន\u200Bអាច\u200Bបង្ហោះ\u200Bរូបភាព\u200Bទៅ minio ទេ។";
        String PERCENT_MATCH_NOT_CONFIG = "ការផ្គូផ្គងភាគរយមិនត្រូវបានកំណត់ទេ។";
    }
}
