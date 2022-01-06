package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.List;


public class OutdatedDataVersionException extends BaseApiException {

    private long requestedDataVersion = -1;
    private long currentDataVersion = -1;

    public OutdatedDataVersionException() {
    }

    public OutdatedDataVersionException(long requestedDataVersion, long currentDataVersion) {
        this.requestedDataVersion = requestedDataVersion;
        this.currentDataVersion = currentDataVersion;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.GONE;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        if (requestedDataVersion != -1) {
            return List.of(new ErrorEntry(
                "The requested data version " + requestedDataVersion + " does not exist anymore. The current data " +
                    "version is " + currentDataVersion + "."
            ));
        } else {
            return List.of(new ErrorEntry(
                "The requested data version does not exist anymore. Please wait a moment and reload."
            ));
        }
    }

}
