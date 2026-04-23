package edu.tcu.cs.projectpulse.system;

public class StatusCode {
    public static final int SUCCESS = 200;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int INVALID_ARGUMENT = 400;
    public static final int CONFLICT = 409;

    private StatusCode() {}
}
