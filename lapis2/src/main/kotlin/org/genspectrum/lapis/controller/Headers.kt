package org.genspectrum.lapis.controller

import org.springframework.http.MediaType

object LapisHeaders {
    const val REQUEST_ID = "X-Request-ID"
    const val LAPIS_DATA_VERSION = "Lapis-Data-Version"
}

object LapisMediaType {
    const val TEXT_X_FASTA_VALUE = "text/x-fasta"
    val TEXT_X_FASTA = MediaType.parseMediaType(TEXT_X_FASTA_VALUE)
    const val TEXT_CSV_VALUE = "text/csv"
    const val TEXT_CSV_WITHOUT_HEADERS_VALUE = "text/csv;$HEADERS_ACCEPT_HEADER_PARAMETER=false"
    const val TEXT_TSV_VALUE = "text/tab-separated-values"
    const val APPLICATION_YAML_VALUE = "application/yaml"
}
