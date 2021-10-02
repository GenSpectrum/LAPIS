package ch.ethz.lapis.api.entity.res;

public class ErrorEntry {

    private String message;

    public ErrorEntry() {
    }

    public ErrorEntry(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public ErrorEntry setMessage(String message) {
        this.message = message;
        return this;
    }
}
