package com.hope.master_service.controller;

import com.hope.master_service.config.AppConfig;
import com.hope.master_service.dto.response.Response;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public class AppController {

    @Autowired
    protected MessageService messageService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    public ResponseEntity<Response> data(Object entity) {
        return data(ResponseCode.ENTITY, null, entity);
    }

    public ResponseEntity<Response> data(ResponseCode code, String message, Object entity) {
        return new ResponseEntity<>(Response.builder()
                .code(code)
                .message(message)
                .data(entity)
                .path(httpServletRequest.getRequestURI())
                .requestId(UUID.randomUUID().toString())
                .version(AppConfig.getVersion()).build(), HttpStatus.OK);
    }

    public ResponseEntity<Response> success(ResponseCode code, String... fields) {
        return new ResponseEntity<>(Response.builder()
                .code(code)
                .message(messageService.getMessage(code, fields))
                .path(httpServletRequest.getRequestURI())
                .requestId(UUID.randomUUID().toString())
                .version(AppConfig.getVersion())
                .build(), code.name().contains("CREATED") ? HttpStatus.CREATED : HttpStatus.OK);
    }
}
