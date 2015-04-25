package main

import java.io.FileNotFoundException

import org.slf4j.LoggerFactory
import org.xml.sax.SAXParseException
import utility._

/**
 * Created by Konstantin on 20/01/2015.
 *
 * Main object which acts as the entry point of the system.
 * The system expects a single to be provided with a single argument
 * which is the path to the XML file containing the details for connecting
 * to the database
 */
object app {

  private val logger = LoggerFactory.getLogger("MainApp")

  def main(args: Array[String]) {
    if (args.isEmpty || (args(0) == null || args(0) == None || args(0).length <= 0)) {
      logger.error("Missing arguments: Unspecified configuration file.")
      logger.error("Program will terminate!")
      System.exit(-1)
    }

    try {
      DBConnectionPool.createPool(args(0))
    } catch {
      case e: FileNotFoundException =>
        logger.info("Configuration file not found or cannot be accessed.")
        logger.error("Exception:", e)
        logger.info("Program will terminate!")
        System.exit(-1)
      case e: NumberFormatException =>
        logger.info("Incorrect connection settings.")
        logger.error("Exception:", e)
        logger.info("Program will terminate!")
        System.exit(-1)
      case e: SAXParseException =>
        logger.info("Incorrect connection settings.")
        logger.error("Exception:", e)
        logger.info("Program will terminate!")
        System.exit(-1)
    }
    Environment.init()
//    Environment.test()

    val iBusMonitor = new iBusMonitor()
    iBusMonitor.start()

    //This is used for performance measurement
    if (Environment.isSystemMonitorActive()) {
      val systemMonitor = new SystemMonitor()
      systemMonitor.start()
    }
  }

}