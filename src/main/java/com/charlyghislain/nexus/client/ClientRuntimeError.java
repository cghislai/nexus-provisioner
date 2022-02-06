package com.charlyghislain.nexus.client;

public class ClientRuntimeError extends RuntimeException {
    public ClientRuntimeError() {
    }

    public ClientRuntimeError(String message) {
        super(message);
    }

    public ClientRuntimeError(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientRuntimeError(Throwable cause) {
        super(cause);
    }

    public ClientRuntimeError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
