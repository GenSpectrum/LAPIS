package org.genspectrum.lapis

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Lapisv2Application

val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    try {
        runApplication<Lapisv2Application>(*args)
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}
