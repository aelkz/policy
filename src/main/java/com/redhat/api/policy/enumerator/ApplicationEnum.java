package com.redhat.api.policy.enumerator;

public enum ApplicationEnum {

    HIT_COUNT("hitCount"),
    HIT_TIMESTAMP("hitTimeStamp"),
    HIT_COUNT_TOTAL("hitCountTotal"),
    HIT_BOUNDARY("hitBoundary"),
    HIT_LAST_429_MILLIS("hitLast429"),
    EMPTY_X_FORWARDED_FOR( "NO_IP"),
    CLIENT_IP("clientIp"),
    GENERAL_PROXY_ERROR_MESSAGE("Proxy processing failed. Message: ");

    private String value;

    ApplicationEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getValueWithMessage(String message) {
        return value+message;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
