package org.genspectrum.lapis

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Lapisv2Application

fun main(args: Array<String>) {
    val referenceGenomeSchemaArgs = ReferenceGenomeSchema.readFromFileFromProgramArgsOrEnv(
        args,
    ).toSpringApplicationArgs()

    try {
        runApplication<Lapisv2Application>(*(args + referenceGenomeSchemaArgs))
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}
