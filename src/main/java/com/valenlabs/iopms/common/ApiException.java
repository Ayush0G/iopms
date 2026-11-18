package com.valenlabs.iopms.common;

import lombok.Getter;

/** Business-rule violation carrying the HTTP status to return. */
@Getter
public class ApiException extends RuntimeException {

    private final transient int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
