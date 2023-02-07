package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.entity.res.SampleDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;


class DetailsFieldTest {
    @Test
    void allFieldsAreInMap() {
        assertThat(DetailsField.FIELD_NAME_TO_DATABASE_COLUMN, aMapWithSize(SampleDetail.Fields.values().length));
    }

    @ParameterizedTest
    @MethodSource("provideFilters")
    void testFilter(List<SampleDetail.Fields> filter, int filteredSize) {

        var result = DetailsField.getDetails(filter);

        assertThat(result, hasSize(filteredSize));
    }

    public static Stream<Arguments> provideFilters() {
        return Stream.of(
            Arguments.of(List.of(), DetailsField.FIELD_NAME_TO_DATABASE_COLUMN.size()),
            Arguments.of(List.of(SampleDetail.Fields.date), 1),
            Arguments.of(List.of(SampleDetail.Fields.date, SampleDetail.Fields.date), 1),
            Arguments.of(List.of(SampleDetail.Fields.date, SampleDetail.Fields.year), 2)
        );
    }

}
