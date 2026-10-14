package com.nft.rights.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 (针对数据库唯一约束冲突、网络异常等统一返回友好的 HTTP 提示)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        String msg = "该作品源文件哈希指纹或 Token ID 已在链上确权存证，请勿重复确权提交！";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.error(409, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统内部异常: " + e.getMessage()));
    }
}
