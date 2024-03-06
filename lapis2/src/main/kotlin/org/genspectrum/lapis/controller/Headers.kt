package org.genspectrum.lapis.controller

object LapisHeaders {
    const val REQUEST_ID = "X-Request-ID"
    const val LAPIS_DATA_VERSION = "Lapis-Data-Version"
}

object LapisMediaType {
    const val TEXT_X_FASTA = "text/x-fasta"
    const val TEXT_CSV = "text/csv"
    const val TEXT_CSV_WITHOUT_HEADERS = "text/csv;$HEADERS_ACCEPT_HEADER_PARAMETER=false"
    const val TEXT_TSV = "text/tab-separated-values"
    const val APPLICATION_YAML = "application/yaml"
}
