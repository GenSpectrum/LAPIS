package org.genspectrum.lapis

import org.genspectrum.lapis.config.ReferenceGenome
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Lapisv2Application

fun main(args: Array<String>) {
    val referenceGenomeArgs = ReferenceGenome.readFromFileFromProgramArgsOrEnv(args).toSpringApplicationArgs()

    try {
        runApplication<Lapisv2Application>(*(args + referenceGenomeArgs))
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}
