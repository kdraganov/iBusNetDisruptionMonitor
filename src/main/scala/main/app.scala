package main

import org.slf4j.LoggerFactory
import utility.{Configuration, FeedThread, SystemMonitor}

/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  private val logger = LoggerFactory.getLogger("APP")

  def main(args: Array[String]) {
    if (args(0) == null || args(0) == None || args(0).length <= 0) {
      logger.error("Missing arguments: Unspecified configuration file.")
    }
    val configuration = Configuration
    configuration.setConfigurationFilePath(args(0))
    configuration.init()

    //TODO:REMOVE
    logger.debug("Loaded below settings:")
    configuration.test()

    //    var buffer = new ArrayBuffer[Observation]()
    //    val list =  Array[String](
    //      "19599;VW1875;BF60VJK;2014/12/01 13:26:43;;89011;734534;186;3;18;3224;410;-0.24404;51.53564;0;;",
    //      "19599;VW1875;BF60VJK;2014/01/01 12:26:43;;89011;734534;186;3;18;3224;410;-0.24404;51.53564;0;;",
    //      "19599;VW1875;BF60VJK;2015/12/01 12:26:43;;89011;734534;186;3;18;3224;410;-0.24404;51.53564;0;;",
    //      "19599;VW1875;BF60VJK;2014/12/01 12:26:43;;89011;734534;186;3;18;3224;410;-0.24404;51.53564;0;;",
    //      "19599;VW1875;BF60VJK;2014/12/01 02:26:43;;89011;734534;186;3;18;3224;410;-0.24404;51.53564;0;;")
    //    for(x <- list){
    //      val temp = new Observation
    //      temp.init(x, "test")
    //      buffer.append(temp)
    //    }
    //
    //    for(item <-buffer){
    //      logger.debug(item.getTimeOfData.toString)
    //    }
    //    logger.debug("\nSORTED:\n")
    ////    buffer = buffer.sortBy(x => x.getTimeOfData)
    //    buffer = Sorting.stableSort(buffer)
    //  //  buffer.sortWith(_.compare(_) > 0)
    //    for(item <-buffer){
    //      logger.debug(item.getTimeOfData.toString)
    //    }

    val iBusMonitor = new iBusMonitor()
    iBusMonitor.start()

    val systemMonitor = new SystemMonitor()
    systemMonitor.start()

    //    val subDir = "February"
    val subDir = "December"
    //SORTED largest to smallest
    val operator = "GOAHD"
    //    val operator = "MITRLNE"
    //    val operator = "ARRIVA"
    //    val operator = "RATP"
    //    val operator = "ABELON"
    //    val operator = "TRTRN"
    //    val operator = "METROB"
    //    val operator = "CTPLUS"
    //    val operator = "SULLVN"
    //    val operator = ""
    val feedThread = new FeedThread(subDir, operator)
    feedThread.start()
  }

}
