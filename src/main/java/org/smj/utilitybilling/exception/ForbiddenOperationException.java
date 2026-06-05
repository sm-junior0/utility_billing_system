package org.smj.utilitybilling.exception;
/**
 * Application exception used for ForbiddenOperation errors.
 */

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
