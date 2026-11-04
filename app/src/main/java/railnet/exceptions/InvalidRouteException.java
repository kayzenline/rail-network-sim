package railnet.exceptions;

public class InvalidRouteException extends TrainsControllerException {
    public InvalidRouteException(String message) {
        super(message, "InvalidRouteException", 400);
    }
}
