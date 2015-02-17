package utility

import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 10/02/2015.
 */
class SystemMonitor extends Thread {

  private val runtime: Runtime = Runtime.getRuntime()
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  //in milliseconds
  private val sleepInterval: Long = 1000 * 5
  private val kb = 1024
  private val mb = kb * kb

  override
  def run(): Unit = {
    while (true) {
      logger.info("Used memory - [{}] Total memory - [{}] Max memory - [{}] Free memory - [{}] Available processors (cores) - [{}]",
        Array[Object](
          getMBString(getUsedMemory()),
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


