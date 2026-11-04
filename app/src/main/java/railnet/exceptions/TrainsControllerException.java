package railnet.exceptions;

public class TrainsControllerException extends Exception {
    private final String type;
    private final int statusCode;

    public TrainsControllerException(String message, String type, int statusCode) {
        super(message);
        this.type = type;
        this.statusCode = statusCode;
    }

    public String getType() {
        return type;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
