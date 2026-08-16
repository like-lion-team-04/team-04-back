package com.likelion.firstbite.firstbiteserver.recognition.vision;

public class VisionRecognitionException extends RuntimeException {
    private final String code;
    private final String requestId;

    public VisionRecognitionException(String code, String message, String requestId) {
        super(message);
        this.code = code;
        this.requestId = requestId;
    }

    public VisionRecognitionException(String code, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.requestId = requestId;
    }

    public String getCode() { return code; }
    public String getRequestId() { return requestId; }
}
