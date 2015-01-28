package main

import lbsl._

/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  def main(args: Array[String]) {
    val directoryToMonitor: String = "E:\\Workspace\\\\iBusMonitorTestDirectory"
    val dataFormatString: String = "yyyy/mm/dd hh:mm:ss"
    val busStopFile: String = "E:\\Workspace\\iBusMonitorTestDirectory\\bus-stops.csv"
    val busRoutesFile: String = "E:\\Workspace\\iBusMonitorTestDirectory\\bus-sequences.csv"
    val iBusMonitor = new iBusMonitor(directoryToMonitor, dataFormatString, busStopFile, busRoutesFile, ';', 2500)
    iBusMonitor.start();
  }

}
