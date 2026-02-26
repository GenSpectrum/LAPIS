package org.genspectrum.lapis.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.spring6.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode

@Configuration
class ThymeleafConfig {
    @Bean
    fun htmlTemplateResolver() =
        SpringResourceTemplateResolver().apply {
            prefix = "classpath:/templates/"
            suffix = ".html"
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
            order = 1
            checkExistence = true
        }

    @Bean
    fun textTemplateResolver() =
        SpringResourceTemplateResolver().apply {
            prefix = "classpath:/templates/"
            suffix = ""
            templateMode = TemplateMode.TEXT
            characterEncoding = "UTF-8"
            order = 2
            checkExistence = true
        }

    @Bean
    fun templateEngine(
        htmlTemplateResolver: SpringResourceTemplateResolver,
        textTemplateResolver: SpringResourceTemplateResolver,
    ) = SpringTemplateEngine().apply {
        setTemplateResolvers(setOf(htmlTemplateResolver, textTemplateResolver))
    }

    @Bean
    fun thymeleafViewResolver(templateEngine: SpringTemplateEngine) =
        ThymeleafViewResolver().apply {
            setTemplateEngine(templateEngine)
            characterEncoding = "UTF-8"
        }
}
