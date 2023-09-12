package org.genspectrum.lapis

import org.genspectrum.lapis.config.ReferenceGenome
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Lapisv2Application

fun main(args: Array<String>) {
    val referenceGenomeArgs = ReferenceGenome.readFromFileFromProgramArgs(args).toSpringApplicationArgs()

    runApplication<Lapisv2Application>(*(args + referenceGenomeArgs))
}
