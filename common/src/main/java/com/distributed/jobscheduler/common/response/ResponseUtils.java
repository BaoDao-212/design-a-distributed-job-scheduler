package com.distributed.jobscheduler.common.response;

public final class ResponseUtils {

    private ResponseUtils() {
    }

    public static <T> ResponseData<T> success(T data) {
        return ResponseData.success(data);
    }

    public static <T> ResponseData<T> failure(String message, String errorCode) {
        return ResponseData.failure(message, errorCode);
    }
}
