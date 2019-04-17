package com.expiremap.lct.client;

/**
 * @author liaochuntao
 */

public enum ExpireTimeType {
    SECOND("s"),
    MINUTE("m"),
    HOUR("h");

    private String type;

    ExpireTimeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
