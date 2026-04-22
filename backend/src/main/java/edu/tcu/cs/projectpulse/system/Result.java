package edu.tcu.cs.projectpulse.system;

public class Result {

    private boolean success;
<<<<<<< HEAD
    private Object data;
    private String message;
    private String error;

    public Result(boolean success, Object data, String message, String error) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
=======
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
>>>>>>> main
}
