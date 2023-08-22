package org.genspectrum.lapis.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.LapisApplicationProperties
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View

@Component
class LapisErrorController(errorAttributes: ErrorAttributes, val notFoundView: NotFoundView) :
    BasicErrorController(errorAttributes, ErrorProperties()) {

    @RequestMapping(produces = [MediaType.TEXT_HTML_VALUE])
    override fun errorHtml(request: HttpServletRequest?, response: HttpServletResponse): ModelAndView {
        val modelAndView = super.errorHtml(request, response)

        response.addHeader("Content-Type", MediaType.TEXT_HTML_VALUE)

        modelAndView.view = notFoundView
        return modelAndView
    }
}

@Component
class NotFoundView(applicationProperties: LapisApplicationProperties) : View {
    private val html: String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Error 404</title>
        </head>
        <body>
            <h1>LAPIS</h1>
            <h3>Page not found!</h3>
            <a href="${applicationProperties.baseUrl.trimEnd('/')}/swagger-ui/index.html">Visit our swagger-ui</a>
        </body>
        </html>
    """.trimIndent()

    override fun render(model: MutableMap<String, *>?, request: HttpServletRequest, response: HttpServletResponse) {
        response.outputStream.write(html.toByteArray())
    }
}
