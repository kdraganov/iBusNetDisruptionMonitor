package main

import org.slf4j.LoggerFactory
import utility.Configuration

/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  def main(args: Array[String]) {
    val logger = LoggerFactory.getLogger("APP")
    if (args(0) == null || args(0) == None || args(0).length <= 0) {
      logger.error("Missing arguments: Unspecified configuration file.")
    }

    val configuration = Configuration
    configuration.setConfigurationFilePath(args(0))
    configuration.init()
    
    //TODO:REMOVE
    logger.debug("Loaded below settings:")
    configuration.test()

    val iBusMonitor = new iBusMonitor()
    iBusMonitor.start();
  }

  private def test(): Unit = {
    val list = Array(15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85)
    for (size <- list) {
      val result = utility.Helper.getRouteSegments(size, 6)
      // val result = utility.Helper.splitArrayIntoSegments(size, 5)
      println("\n [" + size + "] SIZE: " + result.length + "\n************************************\n")
      for (i <- 0 until result.length) {
        println(result(i))
      }
      println("\n************************************\n")
    }

    System.exit(0)
  }

}
