package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.List;


public abstract class BaseApiException extends RuntimeException {

    public abstract HttpStatus getHttpStatus();

    public abstract List<ErrorEntry> getErrorEntries();

}
