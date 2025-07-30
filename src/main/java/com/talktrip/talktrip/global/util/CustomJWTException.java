package com.talktrip.talktrip.global.util;

public class CustomJWTException extends RuntimeException {

    public CustomJWTException(String message) {
        super(message);
    }

    public CustomJWTException(String msg, Throwable cause){
        super(msg, cause);
    }

}


