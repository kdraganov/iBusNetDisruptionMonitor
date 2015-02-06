package main

import java.util
import java.util.Collections

import lbsl.Observation
import org.slf4j.LoggerFactory
import utility.Configuration

/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  def main(args: Array[String]) {
    //    val list =  Array(15,20,25,30,35,40,45,50,55,60,65,70,75,80,85)
    //    for (size <- list) {
    //      val result = utility.Helper.getRouteSegments(size, 6)
    //     // val result = utility.Helper.splitArrayIntoSegments(size, 5)
    //      println("\n [" + size + "] SIZE: " + result.length + "\n************************************\n")
    //      for (i <- 0 until result.length) {
    //        println(result(i))
    //      }
    //      println("\n************************************\n")
    //    }
    //
    //    System.exit(0)

    val logger = LoggerFactory.getLogger("APP")
    logger.info("TEST")
    if (args(0) == null || args(0) == None || args(0).length <= 0) {
      logger.error("Missing arguments: Unspecified configuration file.")
    }

    val configuration = Configuration
    configuration.setConfigurationFilePath(args(0))
    configuration.init()

    //TESTING
    val list = new util.ArrayList[Observation]()
    val input = Array(
      "18238;912;YN55PZR;2014/12/01 02:16:01;;94931;535835;306;3;1;17813;130; 0.01769;51.37758;0;;",
      "18238;912;YN55PZR;2014/12/01 02:16:01;;94931;535835;306;3;2;17813;130; 0.01769;51.37758;0;;",
      "18238;912;YN55PZR;2014/12/01 01:16:01;;94931;535835;306;3;3;17813;130; 0.01769;51.37758;0;;",
      "18108;968;YT59DYO;2014/12/01 02:13:01;;59913;354086;307;3;4;BP2632;0;-0.09134;51.37464;0;;",
      "18110;970;YT59DYS;2014/12/01 02:15:59;;59911;354021;308;3;5;BP154;0;-0.08799;51.36651;0;;"
    )
    for(a <- input){
      val temp = new Observation
      temp.init(a)
      list.add(temp)
    }
    logger.debug("LIST SIZE: {}", list.size())
    logger.debug("Before sorting")
    for (i <- 0 until list.size()) {
      logger.debug("{} Time: {}", list.get(i).getContractRoute, list.get(i).getTimeOfData)
    }
    logger.debug("After sorting")
    Collections.sort(list)

    for (i <- 0 until list.size()) {
      logger.debug("{} Time: {}", list.get(i).getContractRoute, Configuration.getDateFormat().format(list.get(i).getTimeOfData))
    }


    var temp = new Observation
    temp.init("18108;968;YT59DYO;2014/12/01 03:13:01;;59913;354086;307;3;6;BP2632;0;-0.09134;51.37464;0;;")
    list.add(temp)
    temp = new Observation
    temp.init("18108;968;YT59DYO;2014/12/01 03:11:01;;59913;354086;307;3;7;BP2632;0;-0.09134;51.37464;0;;")
    list.add(temp)

    logger.debug("After adding new entries")
    Collections.sort(list)
    for(temp: Observation <- list.toArray(new Array[Observation](list.size())) ){
      logger.debug("{} Time: {}", temp.getContractRoute, Configuration.getDateFormat().format(temp.getTimeOfData))
    }


    System.exit(0)

    //TODO: REMOVE
    configuration.test()
    val iBusMonitor = new iBusMonitor()
    iBusMonitor.start();
  }

}
