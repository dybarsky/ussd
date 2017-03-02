package com.tooploox.ussd.domain;

import org.parceler.Parcel;

import java.util.UUID;

/**
 * Created by mdy on 08.06.16.
 */

@Parcel
public class Ussd {

    private String id = UUID.randomUUID().toString();
    private String code;
    private String regex;
    private String result;
    private String response;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ussd ussd = (Ussd) o;

        return ussd.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
