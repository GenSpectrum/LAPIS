package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;


public class GisaidLimitationException extends BaseApiException {
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        return new ArrayList<>() {{
            add(new ErrorEntry(
                    "This instance of LAPIS uses the GISAID dataset which does not support this operation."
            ));
        }};
    }
}
