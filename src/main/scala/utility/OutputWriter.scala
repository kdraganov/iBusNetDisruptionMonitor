package utility

import java.io.{File, FileNotFoundException, PrintWriter}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import lbsl.BusStop
import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 10/03/2015.
 */
class OutputWriter {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)

  private val fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")
  private val dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
  private val outputDirectory: File = new File("E:\\Workspace\\iBusNetTestDirectory\\DisruptionReports")
  private val outputFilename: String = outputDirectory.getAbsolutePath + "\\Report.csv"
  private val outputFile: File = new File(outputFilename)
  private val header: String = "Route,Direction,FromStopName,FromStopCode,ToStopName,ToStopCode,DisruptionObserved,RouteTotal,Trend,TimeFirstDetected"

  private var prevTime: String = fileDateFormat.format(Calendar.getInstance().getTime())
  private var output: String = ""

  def write(contractRoute: String, direction: String, stopA: BusStop, stopB: BusStop, delayInMinutes: Integer, routeTotalDelayMinutes: Integer, trend: Integer, timeFirstDetected: Date): Unit = {
    output += contractRoute + ","
    output += direction + ","
    output += stopA.getName() + ","
    output += stopA.getCode() + ","
    output += stopB.getName() + ","
    output += stopB.getCode() + ","
    output += delayInMinutes + ","
    output += routeTotalDelayMinutes + ","
    output += trend + ","
    output += dateFormat.format(timeFirstDetected) + "\n"
    logger.trace("{} - {} disrupted section between stop [{}] and stop [{}] of [{}] minutes. ", Array[Object](contractRoute, direction, stopA, stopB, delayInMinutes.toString))
  }

  /**
   * Save to file if output is not empty
   */
  def close(): Unit = {
    if (output.length > 0) {
      save()
    }
  }

  //TODO: java.io.FileNotFoundException: E:\Workspace\iBusNetTestDirectory\DisruptionReports\Report.csv (The process cannot access the file because it is being used by another process)
  def save(): Unit = {
    checkOutputDirectory()
    try {
      if (outputFile.exists()) {
        val newFile: File = new File(outputDirectory.getAbsolutePath + "\\Report_" + prevTime + ".csv")
        outputFile.renameTo(newFile)
      }

      val fileWriter = new PrintWriter(new File(outputFilename))
      fileWriter.write(header + "\n" + output)
      fileWriter.close()
      //TODO: here notify front ent of change
      prevTime = fileDateFormat.format(Calendar.getInstance().getTime())

    } catch {
      case e: FileNotFoundException =>
        logger.error("File {} is in use. Unable to access it.", outputFile.getAbsolutePath)
        logger.error("Exception:", e)
        logger.error("Terminating application.")
        System.exit(1)
    }

  }

  private def checkOutputDirectory() {
    if (!outputDirectory.exists()) {
      logger.warn("Processed directory missing. Trying to create directory [{}].", outputDirectory.getAbsolutePath)
      if (!outputDirectory.mkdir()) {
        logger.error("Failed to create directory [{}].Terminating application.", outputDirectory.getAbsolutePath)
        System.exit(1)
      }
    }
  }

}
