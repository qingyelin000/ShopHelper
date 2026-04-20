package com.shophelper.common.web.handler;

import com.shophelper.common.core.exception.BusinessException;
import com.shophelper.common.core.result.ErrorCode;
import com.shophelper.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", e.getCode(), e.getMessage());
        return Result.<Void>fail(e.getCode(), e.getMessage())
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return Result.<Void>fail(ErrorCode.PARAM_ERROR, message)
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败: {}", message);
        return Result.<Void>fail(ErrorCode.PARAM_ERROR, message)
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        return Result.<Void>fail(ErrorCode.NOT_FOUND)
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        return Result.<Void>fail(ErrorCode.INTERNAL_ERROR)
                .requestId(getRequestId(request));
    }

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }
}
