package com.bit.scmu_taotao.exception;

import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StorageException.class)
    public Result handleStorageException(StorageException e) {
        return Result.fail(500, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return Result.fail(500, "上传文件超过系统限制");
    }

    @ExceptionHandler(Exception.class)
    public Result handleGenericException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.fail(500, "系统异常，请稍后重试");
    }
    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public Result handleBadRequestExceptions(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        // 返回 400 并附带简短提示（避免泄露过多内部信息）
        return Result.fail(400, "请求参数错误或缺失: " + e.getMessage());
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result handleValidationExceptions(Exception ex) {
        BindingResult bindingResult;
        if (ex instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
        } else {
            bindingResult = ((BindException) ex).getBindingResult();
        }
        List<String> messages = bindingResult.getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        String message = String.join("; ", messages);
        log.warn("Validation failed: {}", message);
        return Result.fail(400, message.isEmpty() ? "参数校验失败" : message);
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String path = cv.getPropertyPath() == null ? "" : cv.getPropertyPath().toString();
                    return (path.isEmpty() ? "" : path + ": ") + cv.getMessage();
                })
                .collect(Collectors.joining("; "));
        log.warn("Constraint violations: {}", message);
        return Result.fail(400, message.isEmpty() ? "参数约束校验失败" : message);
    }

    @ExceptionHandler(SensitiveWordException.class)
    public Result handleSensitiveWordException(SensitiveWordException e) {
        return Result.fail(400, e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Result handleStompException(Exception e) {
        log.error("STOMP message processing failed", e);
        return Result.fail(500, "message processing failed");
    }
}