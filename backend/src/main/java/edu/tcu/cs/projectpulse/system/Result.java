package edu.tcu.cs.projectpulse.system;

public class Result {

    private boolean success;
    private int code;
    private String message;
    private Object data;

    public Result(boolean success, int code, String message, Object data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(boolean success, int code, String message) {
        this(success, code, message, null);
    }

    public boolean isSuccess() { return success; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
