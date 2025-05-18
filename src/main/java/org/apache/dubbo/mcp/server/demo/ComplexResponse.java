package org.apache.dubbo.mcp.server.demo;

public class ComplexResponse {
    private String message;
    private boolean success;
    private int code;

    // Default constructor
    public ComplexResponse() {
    }
    
    public ComplexResponse(String message, boolean success, int code) {
        this.message = message;
        this.success = success;
        this.code = code;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ComplexResponse{" +
                "message='" + message + '\'' +
                ", success=" + success +
                ", code=" + code +
                '}';
    }
} 