package org.genspectrum.lapis.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.ThreadMXBean

@Component
class MemoryLogger {
    private val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()

    @Scheduled(fixedRate = 5000)
    fun logMemoryUsage() {
        // Get heap memory usage
        val heapMemoryUsage = memoryMXBean.heapMemoryUsage
        val heapUsedMB = heapMemoryUsage.used / (1024 * 1024)
        val heapCommittedMB = heapMemoryUsage.committed / (1024 * 1024)
        val heapMaxMB = heapMemoryUsage.max / (1024 * 1024)

        // Get non-heap memory usage
        val nonHeapMemoryUsage = memoryMXBean.nonHeapMemoryUsage
        val nonHeapUsedMB = nonHeapMemoryUsage.used / (1024 * 1024)
        val nonHeapCommittedMB = nonHeapMemoryUsage.committed / (1024 * 1024)
        val nonHeapMaxMB = nonHeapMemoryUsage.max / (1024 * 1024)

        // Get stack size (approximated by the total thread count and the stack size per thread)
        val threadCount = threadMXBean.threadCount
        val stackSizePerThread = (512 * 1024).toLong() // Default stack size is 512 KB, adjust if different
        val totalStackSizeMB = (threadCount * stackSizePerThread) / (1024 * 1024)

        // Log memory usage
        println("Heap Memory Usage (MB):")
        println("  Used: $heapUsedMB MB")
        println("  Committed: $heapCommittedMB MB")
        println("  Max: $heapMaxMB MB")

        println("Non-Heap Memory Usage (MB):")
        println("  Used: $nonHeapUsedMB MB")
        println("  Committed: $nonHeapCommittedMB MB")
        println("  Max: $nonHeapMaxMB MB")

        println("Stack Memory Usage (MB):")
        println("  Total Stack Size: $totalStackSizeMB MB")
        println("  Thread Count: $threadCount")
    }
}
