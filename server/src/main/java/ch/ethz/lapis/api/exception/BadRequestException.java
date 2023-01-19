package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.List;

public class BadRequestException extends BaseApiException {

    private final String message;

    public BadRequestException(String message) {
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        return List.of(new ErrorEntry(message));
    }
}
