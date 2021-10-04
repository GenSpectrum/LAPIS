package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import java.util.List;
import org.springframework.http.HttpStatus;


public abstract class BaseApiException extends RuntimeException {

    public abstract HttpStatus getHttpStatus();

    public abstract List<ErrorEntry> getErrorEntries();

}
