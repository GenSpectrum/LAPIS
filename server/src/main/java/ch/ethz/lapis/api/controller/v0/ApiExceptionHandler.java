package ch.ethz.lapis.api.controller.v0;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import ch.ethz.lapis.api.entity.res.V0Response;
import ch.ethz.lapis.api.exception.BaseApiException;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseApiException.class)
    protected ResponseEntity<V0Response<Object>> handleExpectedExceptions(BaseApiException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(new V0Response<>().setErrors(ex.getErrorEntries()));
    }

    @ExceptionHandler(Throwable.class)
    protected ResponseEntity<V0Response<Object>> handleUnexpectedExceptions(Throwable ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new V0Response<>().setErrors(new ArrayList<>() {{
                add(new ErrorEntry(
                    "Unexpected error: Please report it to https://github.com/cevo-public/LAPIS/issues"
                ));
            }}));
    }

}
