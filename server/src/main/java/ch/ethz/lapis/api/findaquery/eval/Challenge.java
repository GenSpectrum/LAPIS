package ch.ethz.lapis.api.findaquery.eval;

import ch.ethz.lapis.api.findaquery.BackgroundFilter;
import java.util.List;

/**
 *
 * @param sequences The IDs of the sequences for which we are looking for a query
 * @param backgroundFilter A filter defining the set of sequences that should be considered. Sequences passing the
 *                         "backgroundFilter" and not in "sequences" are the unwanted sequences (i.e., sequences
 *                         that we do not want to query). Overall, the algorithm is going to work on the union of
 *                         the sequences passing the "backgroundFilter" and the "sequences".
 * @param additionalMetadata Additional metadata that are specific to the type of challenge and are used by the
 *                           different result reporting functions. It could e.g. contain the ID of a CoV-Spectrum
 *                           collection from which the variant was taken.
 * @param <T>
 */
public record Challenge<T>(
    List<String> sequences,
    BackgroundFilter backgroundFilter,
    T additionalMetadata
) {

}
