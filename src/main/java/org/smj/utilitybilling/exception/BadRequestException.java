package org.smj.utilitybilling.exception;
/**
 * Application exception used for BadRequest errors.
 */

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
