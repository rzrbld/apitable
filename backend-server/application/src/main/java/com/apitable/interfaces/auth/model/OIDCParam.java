package com.apitable.interfaces.auth.model;

public class OIDCParam {
    private String oidcCode;

    public OIDCParam(String oidcCode) {
        this.oidcCode = oidcCode;
    }

    public String getOidcCode() {
        return oidcCode;
    }

    public void setOidcCode(String oidcCode) {
        this.oidcCode = oidcCode;
    }
}