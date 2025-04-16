package mesfavoris.commons;

public class Status {
    public static final int OK = 0;
    public static final Status OK_STATUS = new Status(Severity.OK, OK, "", null);

    private final Severity severity;
    private final int code;
    private final String message;
    private final Throwable exception;

    public enum Severity {
        OK,
        ERROR,
        INFO,
        WARNING
    }

    public Status(Severity severity, int code, String message, Throwable exception) {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.exception = exception;
    }

    public Severity getSeverity() {
        return severity;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isOk() {
        return getSeverity() == Severity.OK;
    }

}
