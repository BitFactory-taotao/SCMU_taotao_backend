package com.bit.scmu_taotao.exception;

public class SensitiveWordException extends RuntimeException {
    private String detectedWord;

    public SensitiveWordException(String message, String detectedWord) {
        super(message);
        this.detectedWord = detectedWord;
    }

    public String getDetectedWord() {
        return detectedWord;
    }
}
