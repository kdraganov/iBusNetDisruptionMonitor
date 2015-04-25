package utility

import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 10/02/2015.
 *
 * Class used for gathering memory usage information,
 * throughout the execution of the system. Used for
 * stress testing measurements.
 */
class SystemMonitor extends Thread {

  private val runtime: Runtime = Runtime.getRuntime()
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  //in milliseconds
  private val sleepInterval: Long = 1000 * 30
  private val kb = 1024
  private val mb = kb * kb
  private var maxUsedMemory: Long = 0

  override
  def run(): Unit = {
    while (true) {
      val usedMemory = getUsedMemory()
      if (usedMemory > maxUsedMemory) {
        maxUsedMemory = usedMemory
      }
      logger.info("Used memory - [{}] Max used memory - [{}] Total memory - [{}] Max memory - [{}] Free memory - [{}] Available processors (cores) - [{}]",
        Array[Object](
          getMBString(usedMemory),
          getMBString(maxUsedMemory),
          getMBString(runtime.totalMemory()),
          if (runtime.maxMemory() == Long.MaxValue) "No Limit" else getMBString(runtime.maxMemory()),
          getMBString(runtime.freeMemory()),
          runtime.availableProcessors().toString))

      try {
        Thread.sleep(sleepInterval)
      } catch {
        case e: InterruptedException => logger.error("iBusMonitorThread interrupted:", e)
      }
    }
  }

  private def getUsedMemory(): Long = {
    runtime.totalMemory() - runtime.freeMemory()
  }

  private def getMBString(value: Long): String = {
    val temp = value / mb
    return temp.toString + "MB"
  }
}


