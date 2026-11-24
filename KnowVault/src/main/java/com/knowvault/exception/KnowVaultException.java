package com.knowvault.exception;

public class KnowVaultException extends RuntimeException {
    public KnowVaultException(String message) { super(message); }
    public KnowVaultException(String message, Throwable cause) { super(message, cause); }
}
