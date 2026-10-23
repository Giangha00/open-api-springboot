package com.sem4.openapispringboot.exception;

public class PreconditionFailedException extends RuntimeException {
    private final String expectedETag;
    private final String currentETag;

    public PreconditionFailedException(String expectedETag, String currentETag) {
        super(String.format("Precondition Failed: If-Match version '%s' does not match current resource version '%s'", expectedETag, currentETag));
        this.expectedETag = expectedETag;
        this.currentETag = currentETag;
    }

    public String getExpectedETag() {
        return expectedETag;
    }

    public String getCurrentETag() {
        return currentETag;
    }
}
