package com.bit.scmu_taotao.exception;

import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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