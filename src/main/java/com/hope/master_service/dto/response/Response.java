package com.hope.master_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Value
@Builder
public class Response {

    @Builder.Default
    private Date date = new Date();

    private ResponseCode code;

    @JsonDeserialize(as = String.class)
    private Object message;

    @JsonDeserialize(as = String.class)
    private Object data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> errors;

    private String path;

    private String requestId;

    private String version;
}
