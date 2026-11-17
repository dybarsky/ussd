package com.tooploox.ussd.domain;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by mdy on 08.06.16.
 */

public class Ussd implements Serializable, Cloneable {

    // Pins the UID computed for the originally released class so stored (serialized) data stays readable.
    private static final long serialVersionUID = -2632603246578927450L;

    private String id;
    private String code;
    private String response;

    public Ussd(String id) {
        this.id = id;
    }

    public Ussd() {
        id = UUID.randomUUID().toString();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getId() {
        return id;
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

    @Override
    public Ussd clone() {
        Ussd ussd = new Ussd(id);
        ussd.setCode(code);
        ussd.setResponse(response);
        return ussd;
    }
}
