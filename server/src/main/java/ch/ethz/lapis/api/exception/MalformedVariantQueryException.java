package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.List;

public class MalformedVariantQueryException extends BaseApiException {
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        return List.of(new ErrorEntry(
            "The variantQuery is malformed."
        ));
    }
}
