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
    //    logger.debug("Loaded below settings:")
    //    configuration.test()

    val iBusMonitor = new iBusMonitor()
    iBusMonitor.start();
  }
}
