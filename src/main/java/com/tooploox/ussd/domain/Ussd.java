package com.tooploox.ussd.domain;

import org.parceler.Parcel;

/**
 * Created by mdy on 08.06.16.
 */

@Parcel
public class Ussd {

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

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Ussd ussd = (Ussd) o;
//
//        if (code != null ? !code.equals(ussd.code) : ussd.code != null) return false;
//        if (regex != null ? !regex.equals(ussd.regex) : ussd.regex != null) return false;
//        if (result != null ? !result.equals(ussd.result) : ussd.result != null) return false;
//        return response != null ? response.equals(ussd.response) : ussd.response == null;
//
//    }
//
//    @Override
//    public int hashCode() {
//        int result1 = code != null ? code.hashCode() : 0;
//        result1 = 31 * result1 + (regex != null ? regex.hashCode() : 0);
//        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
//        result1 = 31 * result1 + (response != null ? response.hashCode() : 0);
//        return result1;
//    }
}
