package org.genspectrum.lapis

import org.genspectrum.lapis.config.ACTIVE_VIEW_REQUEST_ATTRIBUTE
import org.genspectrum.lapis.config.ViewRegistry
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class ActiveViewTestExecutionListener : AbstractTestExecutionListener() {
    override fun beforeTestMethod(testContext: TestContext) {
        val viewRegistry =
            testContext.applicationContext.getBeanProvider(ViewRegistry::class.java).ifAvailable ?: return
        val request = MockHttpServletRequest().apply {
            setAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE, viewRegistry.first())
        }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    override fun afterTestMethod(testContext: TestContext) {
        RequestContextHolder.resetRequestAttributes()
    }
}
