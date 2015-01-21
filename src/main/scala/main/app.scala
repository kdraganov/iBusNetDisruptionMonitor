package main


/**
 * Created by Konstantin on 20/01/2015.
 */
object app {

  def main(args: Array[String]) {
    val iBusMonitor = new iBusMonitor("E:\\Workspace\\SampleTFLData", "yyyy/mm/dd hh:mm:ss");
    iBusMonitor.start();
  }

}
