package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class UnsupportedOrdering extends BaseApiException {

    private final String orderByValue;

    public UnsupportedOrdering(String orderByValue) {
        this.orderByValue = orderByValue;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        return new ArrayList<>() {{
            add(new ErrorEntry("The value provided in orderBy is not supported: " + orderByValue));
        }};
    }
}
