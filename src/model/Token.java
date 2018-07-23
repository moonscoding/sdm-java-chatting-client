package model;

import java.io.Serializable;

public class Token implements Serializable {
    public String method;
    public String message;
    public Token(String method, String message) {
        this.method = method;
        this.message = message;
    }
}
