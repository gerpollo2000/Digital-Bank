package io.digisic.bank.util;

import com.fasterxml.jackson.databind.JsonNode;

public class DTPUtil {

    private JsonNode responseData;

    public DTPUtil(JsonNode responseData) {
        this.responseData = responseData;
    }

    public boolean getAssertion(String assertion) {
        if (responseData.get("assertion_claims") == null)
            return false;

        if (responseData.get("assertion_claims").get(assertion) == null)
            return false;

        return Boolean.valueOf(responseData.get("assertion_claims").get(assertion).get("result").asText());
    }
}
