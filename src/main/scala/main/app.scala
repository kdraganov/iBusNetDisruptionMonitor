package main

import org.slf4j.LoggerFactory
import utility._

/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  //    val subDir = "February"
  val subDir = "Demo"
  //    val subDir = "December"
  //SORTED largest to smallest
  private val operator = ""
  //    val operator = "GOAHD"
  //    val operator = "MITRLNE"
  //    val operator = "ARRIVA"
  //    val operator = "RATP"
  //    val operator = "ABELON"
  //    val operator = "TRTRN"
  //    val operator = "METROB"
  //    val operator = "CTPLUS"
  //    val operator = "SULLVN"

  private val logger = LoggerFactory.getLogger("APP")

  def main(args: Array[String]) {
    if (args(0) == null || args(0) == None || args(0).length <= 0) {
      logger.error("Missing arguments: Unspecified configuration file.")
    }
    DBConnectionPool.createPool(args(0))
    Environment.init()
    Environment.test()

    val iBusMonitor = new iBusMonitor()
    iBusMonitor.start()

    val systemMonitor = new SystemMonitor()
    systemMonitor.start()
    try {
      Thread.sleep(10000)
    } catch {
      case e: InterruptedException => logger.error("Thread interrupted:", e)
    }

    val feedThread = new FeedThread(subDir, operator)
    feedThread.start()
  }

}