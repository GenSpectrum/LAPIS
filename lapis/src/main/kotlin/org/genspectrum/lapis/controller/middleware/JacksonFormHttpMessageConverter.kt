package org.genspectrum.lapis.controller.middleware

import com.fasterxml.jackson.databind.ObjectMapper
import org.genspectrum.lapis.util.tryToGuessTheType
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.stereotype.Component

@Component
class JacksonFormHttpMessageConverter(
    private val objectMapper: ObjectMapper,
) : AbstractHttpMessageConverter<Any>(MediaType.APPLICATION_FORM_URLENCODED) {
    private val formHttpMessageConverter = FormHttpMessageConverter()

    override fun canWrite(
        clazz: Class<*>,
        mediaType: MediaType?,
    ): Boolean = false

    override fun supports(clazz: Class<*>): Boolean = true

    override fun writeInternal(
        t: Any,
        outputMessage: HttpOutputMessage,
    ): Unit = throw NotImplementedError("This class should never need to write.")

    override fun readInternal(
        clazz: Class<out Any>,
        inputMessage: HttpInputMessage,
    ): Any {
        val multiValueMap = formHttpMessageConverter.read(null, inputMessage)

        val mapValues = multiValueMap.mapValues(::tryToGuessTheType)

        val json = objectMapper.writeValueAsString(mapValues)

        return objectMapper.readValue(json, clazz)
    }
}
