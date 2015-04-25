package utility

import java.io.{File, FileNotFoundException, PrintWriter}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import lbsl.BusStop
import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 10/03/2015.
 *
 * Class used for writing the output of the system to CSV files.
 * This however has become obsolete as the system has moved to use a database.
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

  //TODO: need to add quotes around the bus stop names in case they have commas
  def write(contractRoute: String, direction: String, stopA: BusStop, stopB: BusStop, delayInMinutes: Integer, routeTotalDelayMinutes: Integer, trend: Integer, timeFirstDetected: Date): Unit = {
    output += contractRoute + ","
    output += direction + ","
    output += "\"" + stopA.getName() + "\","
    output += stopA.getCode() + ","
    output += "\"" + stopB.getName() + "\","
    output += stopB.getCode() + ","
    output += delayInMinutes + ","
    output += routeTotalDelayMinutes + ","
    output += trend + ","
    output += dateFormat.format(timeFirstDetected) + "\n"
    logger.trace("{} - {} disrupted section between stop [{}] and stop [{}] of [{}] minutes. ", Array[Object](contractRoute, direction, stopA.getCode(), stopB.getCode(), delayInMinutes.toString))
  }

  /**
   * Save to file if output is not empty
   */
  def close(): Unit = {
    checkOutputDirectory()
    try {
      renameCurrent()
    } catch {
      case e: FileNotFoundException =>
        logger.error("File {} is in use. Unable to access it.", outputFile.getAbsolutePath)
        logger.error("Exception:", e)
        logger.error("Terminating application.")
        System.exit(1)
    }
    if (output.length > 0) {
      save()
    }
  }

  private def renameCurrent(): Unit = {
    try {
      if (outputFile.exists()) {
        val newFile: File = new File(outputDirectory.getAbsolutePath + "\\Report_" + prevTime + ".csv")
        outputFile.renameTo(newFile)
      }

    } catch {
      case e: FileNotFoundException =>
        logger.error("File {} is in use. Unable to access it.", outputFile.getAbsolutePath)
        logger.error("Exception:", e)
        logger.error("Terminating application.")
        System.exit(1)
    }
  }

  private def save(): Unit = {
    val fileWriter = new PrintWriter(new File(outputFilename))
    fileWriter.write(header + "\n" + output)
    fileWriter.close()
    prevTime = fileDateFormat.format(Calendar.getInstance().getTime())
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