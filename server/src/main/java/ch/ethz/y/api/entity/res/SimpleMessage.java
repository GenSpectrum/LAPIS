package ch.ethz.y.api.entity.res;

public class SimpleMessage {
    private String message;

    public SimpleMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public SimpleMessage setMessage(String message) {
        this.message = message;
        return this;
    }
}
