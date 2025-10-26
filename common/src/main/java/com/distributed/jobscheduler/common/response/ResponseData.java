package com.distributed.jobscheduler.common.response;

import java.time.Instant;

public class ResponseData<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;
    private final Instant timestamp;

    private ResponseData(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public static <T> ResponseData<T> success(T data) {
        return new ResponseData<>(true, data, null, null);
    }

    public static <T> ResponseData<T> failure(String message, String errorCode) {
        return new ResponseData<>(false, null, message, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
