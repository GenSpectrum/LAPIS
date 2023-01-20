package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import ch.ethz.lapis.api.entity.res.V1Response;
import ch.ethz.lapis.api.exception.BaseApiException;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseApiException.class)
    protected ResponseEntity<V1Response<Object>> handleExpectedExceptions(BaseApiException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(new V1Response<>().setErrors(ex.getErrorEntries()));
    }

    @ExceptionHandler(Throwable.class)
    protected ResponseEntity<V1Response<Object>> handleUnexpectedExceptions(Throwable ex) {
        log.error("Unexpected error:" + ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new V1Response<>().setErrors(new ArrayList<>() {{
                add(new ErrorEntry(
                    "Unexpected error: Please report it to https://github.com/cevo-public/LAPIS/issues"
                ));
            }}));
    }

}
