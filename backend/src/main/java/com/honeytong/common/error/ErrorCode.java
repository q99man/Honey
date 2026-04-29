package com.honeytong.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    PHONE_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "전화 인증이 필요합니다."),
    REGION_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "지역 인증이 필요합니다."),
    INVALID_REGION_CHANGE(HttpStatus.BAD_REQUEST, "지역 변경 요청이 올바르지 않습니다."),
    POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "정책을 만족하지 않습니다."),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "외부 서비스 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
