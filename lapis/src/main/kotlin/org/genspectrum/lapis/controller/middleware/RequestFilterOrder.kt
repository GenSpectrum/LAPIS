package org.genspectrum.lapis.controller.middleware

const val DATA_FORMAT_FILTER_ORDER = 0
const val DOWNLOAD_AS_FILE_FILTER_ORDER = DATA_FORMAT_FILTER_ORDER + 1
const val COMPRESSION_FILTER_ORDER = DOWNLOAD_AS_FILE_FILTER_ORDER - 1
const val DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER = DATA_FORMAT_FILTER_ORDER - 3
