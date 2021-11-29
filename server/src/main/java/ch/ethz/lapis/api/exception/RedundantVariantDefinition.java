package ch.ethz.lapis.api.exception;

import ch.ethz.lapis.api.entity.res.ErrorEntry;
import org.springframework.http.HttpStatus;

import java.util.List;

public class RedundantVariantDefinition extends BaseApiException {
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public List<ErrorEntry> getErrorEntries() {
        return List.of(new ErrorEntry(
            "Please specify the variant either by using the fields pangoLineage, nextstrainClade, gisaidClade, " +
                "aaMutations and nucMutations, or by using variantQuery - don't use both at the same time."
        ));
    }
}
