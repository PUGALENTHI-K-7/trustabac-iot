package com.trustabac.iot.exception;

public class BlockchainUnavailableException extends RuntimeException {
    public BlockchainUnavailableException(String message) {
        super(message);
    }

    public BlockchainUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
