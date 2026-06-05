package org.smj.utilitybilling.exception;
/**
 * Application exception used for ResourceNotFound errors.
 */

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
