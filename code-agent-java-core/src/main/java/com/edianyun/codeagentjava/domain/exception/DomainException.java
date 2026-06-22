package com.edianyun.codeagentjava.domain.exception;

/**
 * 领域异常基类，所有领域层异常的父类。
 * 用于区分领域逻辑异常与基础设施/技术异常。
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
