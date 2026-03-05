package com.hope.master_service.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hope.master_service.config.AppConfig;
import com.hope.master_service.dto.response.Response;
import com.hope.master_service.dto.response.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



@ControllerAdvice
@Slf4j
public class AppExceptionHandler extends ResponseEntityExceptionHandler {

    private static final HttpHeaders httpHeaders = new HttpHeaders();

    @ExceptionHandler(HopeException.class)
    protected ResponseEntity<Object> handleCustomException(HopeException exception, WebRequest request) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        switch (exception.getErrorCode()) {
            case UNSUPPORTED_MEDIA_TYPE -> httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case NOT_FOUND -> httpStatus = HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR, DB_ERROR, IAM_ERROR, AWS_ERROR -> httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAUTHORIZED -> httpStatus = HttpStatus.UNAUTHORIZED;
        }
        logException("Custom Exception : ", exception);
        return handleExceptionInternal(exception, buildResponse(exception.getErrorCode(), exception.getMessage(), request), httpHeaders, httpStatus, request);
    }

    private void logException(String prefix, Exception exception) {
        StringWriter buffer = new StringWriter();
        exception.printStackTrace(new PrintWriter(buffer));
        log.error(prefix + buffer.toString().replace("\n", "\r"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException exception, WebRequest request) {
        // TODO - Refactor this logic
        Map<Integer, List<String>> violationsByRow = new HashMap<>();

        exception.getConstraintViolations().forEach(violation -> {
            Matcher matcher = Pattern.compile("\\[(\\d+)\\]").matcher(violation.getPropertyPath().toString());
            if (matcher.find()) {
                int rowIndex = Integer.parseInt(matcher.group(1));
                String message = new StringBuilder("row ").append(rowIndex + 1).append(": ").append(violation.getMessage()).toString();
                violationsByRow.computeIfAbsent(rowIndex, k -> new ArrayList<>()).add(message);
            } else {
                violationsByRow.computeIfAbsent(0, k -> new ArrayList<>()).add(violation.getMessage());
            }
        });

        List<String> violations = violationsByRow.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
        return handleExceptionInternal(exception, buildResponse(ResponseCode.BAD_REQUEST,
                new StringBuilder("Constraint violation: ").append(String.join("; ", violations)).toString(), request), httpHeaders, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDaoException(DataIntegrityViolationException exception, WebRequest request) {
        logException("Database Exception : ", exception);
        return handleExceptionInternal(exception, buildResponse(ResponseCode.DB_ERROR,
                        NestedExceptionUtils.getMostSpecificCause(exception).getMessage(), request),
                httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logException("Method Args Exception : ", exception);
        String message;
        try {
            message = exception.getBindingResult().getFieldError().getDefaultMessage();
        } catch (Exception e) {
            message = exception.getBindingResult().toString();
        }
        return handleExceptionInternal(exception, buildResponse(ResponseCode.BAD_REQUEST, message, request), httpHeaders, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logException("HttpMessageNotReadable Exception : ", exception);
        String message = null;
        if (exception.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ifx = (InvalidFormatException) exception.getCause();
            if (ifx.getTargetType() != null && ifx.getTargetType().isEnum()) {
                message = String.format("Invalid enum value for the field: '%s'. The value must be one of: %s.",
                        ifx.getPath().get(ifx.getPath().size() - 1).getFieldName(), Arrays.toString(ifx.getTargetType().getEnumConstants()));
            }
        }
        return handleExceptionInternal(exception, buildResponse(ResponseCode.BAD_REQUEST, message, request), headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handle(Exception exception, WebRequest request) {
//        logException("Generic Exception : ", exception);
//        return handleExceptionInternal(exception, buildResponse(ResponseCode.INTERNAL_ERROR, exception.getMessage(), request),
//                httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR, request);

        logException("Generic Exception : ", exception);

        String rawMessage = exception.getMessage();
        String friendlyMessage = exception.getMessage();

        if (rawMessage != null) {
            // Detect schema-related or relation-not-found issues
            if (rawMessage.contains("relation") && rawMessage.contains("does not exist")) {
                friendlyMessage = "Requested data is not available. Please check your tenant configuration.";
            } else if (rawMessage.toLowerCase().contains("schema") && rawMessage.toLowerCase().contains("not found")) {
                friendlyMessage = "Tenant information is missing or invalid.";
            }
        }

        return handleExceptionInternal(exception,
                buildResponse(ResponseCode.INTERNAL_ERROR, friendlyMessage, request),
                httpHeaders,
                HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException exception, WebRequest request, HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return handleExceptionInternal(exception, buildResponse(ResponseCode.ACCESS_DENIED, "Access denied", request),
                    httpHeaders, HttpStatus.FORBIDDEN, request);
        } catch (Exception e) {
            return handleExceptionInternal(exception, buildResponse(ResponseCode.SERVICE_UNAVAILABLE, exception.getMessage(), request),
                    httpHeaders, HttpStatus.SERVICE_UNAVAILABLE, request);
        }
    }

    @ExceptionHandler(HttpClientErrorException.UnsupportedMediaType.class)
    public ResponseEntity<Object> unsupportedMediaTypeException(HttpClientErrorException.UnsupportedMediaType exception, WebRequest request) {
        return handleExceptionInternal(exception, buildResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", request),
                httpHeaders, HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    private Response buildResponse(ResponseCode code, String message, WebRequest request) {
        return Response.builder()
                .code(code)
                .message(message)
                .path(request.getDescription(true))
                .requestId(UUID.randomUUID().toString())
                .errors(null)
                .version(AppConfig.getVersion())
                .build();
    }
}