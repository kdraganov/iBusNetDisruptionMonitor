package main

import lbsl._
/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  def main(args: Array[String]) {
    var network = new Network
    network.init("E:\\Workspace\\iBusMonitorTestDirectory\\bus-stops.csv", "E:\\Workspace\\iBusMonitorTestDirectory\\bus-sequences.csv")
//    val iBusMonitor = new iBusMonitor("E:\\Workspace\\\\iBusMonitorTestDirectory", "yyyy/mm/dd hh:mm:ss");
//    iBusMonitor.start();
  }

}
