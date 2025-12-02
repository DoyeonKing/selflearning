package com.example.springboot.exception;

public class CustomException extends RuntimeException {
    private String msg;
    private String code;

    public CustomException(String msg,String code) {
        this.code=code;
        this.msg=msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
