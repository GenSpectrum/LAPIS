package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.req.DataFormat;
import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.List;


public class UnsupportedDataFormatException extends BaseApiException {

    private final DataFormat dataFormat;

    public UnsupportedDataFormatException(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        return List.of(new ErrorEntry(
            "The requested data format \"" + dataFormat + "\" is not supported by this endpoint."
        ));
    }

}
