package com.hope.master_service.exception;

import com.hope.master_service.dto.response.ResponseCode;
import lombok.Getter;

@Getter
public class HopeException extends Exception {

    private ResponseCode errorCode;
    private String[] fields;
    private Exception exception;

    public HopeException(ResponseCode unsupportedMediaType) {
        super("Failed to do operation");
        this.errorCode = ResponseCode.INTERNAL_ERROR;
        this.exception = new RuntimeException();
    }

    public HopeException(ResponseCode code, String message, String... fields) {
        super(message);
        this.errorCode = code;
        this.fields = fields;
    }

    public HopeException(Exception exception) {
        super(exception.getLocalizedMessage());
        this.errorCode = ResponseCode.INTERNAL_ERROR;
        this.exception = exception;
    }
}
