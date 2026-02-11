package org.genspectrum.lapis.util

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ContentDispositionUtilsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("getContentDispositionTestCases")
    fun `generateContentDisposition produces correct format`(
        description: String,
        filename: String,
        expected: String,
    ) {
        val actual = generateContentDisposition(filename)
        assertThat(actual, `is`(expected))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAsciiFilteringTestCases")
    fun `toAsciiFilename filters correctly`(
        description: String,
        input: String,
        expected: String,
    ) {
        val actual = toAsciiFilename(input)
        assertThat(actual, `is`(expected))
    }

    companion object {
        @JvmStatic
        fun getContentDispositionTestCases() =
            listOf(
                Arguments.of(
                    "ASCII filename",
                    "aggregated.json",
                    "attachment; filename=aggregated.json; filename*=UTF-8''aggregated.json",
                ),
                Arguments.of(
                    "UTF-8 filename with accents",
                    "données.json",
                    "attachment; filename=donnes.json; filename*=UTF-8''donn%C3%A9es.json",
                ),
                Arguments.of(
                    "filename with space",
                    "my file.csv",
                    "attachment; filename=my file.csv; filename*=UTF-8''my%20file.csv",
                ),
                Arguments.of(
                    "filename with Chinese characters",
                    "测试.json",
                    "attachment; filename=.json; filename*=UTF-8''%E6%B5%8B%E8%AF%95.json",
                ),
                Arguments.of(
                    "compressed file",
                    "data.json.gz",
                    "attachment; filename=data.json.gz; filename*=UTF-8''data.json.gz",
                ),
            )

        @JvmStatic
        fun getAsciiFilteringTestCases() =
            listOf(
                Arguments.of("ASCII unchanged", "test.json", "test.json"),
                Arguments.of("Remove accents", "données.json", "donnes.json"),
                Arguments.of("Remove Chinese", "文件.txt", ".txt"),
                Arguments.of("Preserve space", "my file.csv", "my file.csv"),
                Arguments.of("Mixed content", "file_测试_2024.json", "file__2024.json"),
            )
    }
}
