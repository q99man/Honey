package com.honeytong.common.api;

public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String message
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }

    public static ApiResponse<Void> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message);
    }
}
