package org.genspectrum.lapis.util

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class CachedBodyHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    private val cachedBody: ByteArray by lazy {
        val inputStream: InputStream = request.inputStream
        val byteArrayOutputStream = ByteArrayOutputStream()

        inputStream.copyTo(byteArrayOutputStream)
        byteArrayOutputStream.toByteArray()
    }

    @Throws(IOException::class)
    override fun getInputStream(): ServletInputStream {
        return CachedBodyServletInputStream(ByteArrayInputStream(cachedBody))
    }

    private inner class CachedBodyServletInputStream(private val cachedInputStream: ByteArrayInputStream) :
        ServletInputStream() {

        override fun isFinished(): Boolean {
            return cachedInputStream.available() == 0
        }

        override fun isReady(): Boolean {
            return true
        }

        override fun setReadListener(listener: ReadListener) {
            throw UnsupportedOperationException("setReadListener is not supported")
        }

        @Throws(IOException::class)
        override fun read(): Int {
            return cachedInputStream.read()
        }
    }
}
