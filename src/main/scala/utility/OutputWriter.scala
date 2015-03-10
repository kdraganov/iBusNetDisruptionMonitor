package utility

import java.io.{File, FileNotFoundException, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Calendar

import lbsl.Disruption
import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 10/03/2015.
 */
class OutputWriter {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)

  private val dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")
  private val outputDirectory: File = new File("E:\\Workspace\\iBusNetTestDirectory\\DisruptionReports")
  private val outputFilename: String = outputDirectory.getAbsolutePath + "\\Report.csv"
  private val outputFile: File = new File(outputFilename)


  private var prevTime: String = dateFormat.format(Calendar.getInstance().getTime())

  private var output: String = ""

  def write(contractRoute: String, disruption: Disruption): Unit = {
    val stopA = busStopMap.getOrElse(disruption.getSectionStartBusStop, null).getName()
    val stopB = busStopMap.getOrElse(disruption.getSectionEndBusStop, null).getName()
    output += "Route,Direction,SectionStart,SectionEnd,DisruptionObserved,RouteTotal,Trend,TimeFirstDetected\n"
    output += contractRoute + ","
    output += Direction + ","
    output += SectionStart + ","
    output += SectionEnd + ","
    output += disruption.getDelayInMinutes + ","
    output += RouteTotal + ","
    output += disruption.getTrend + ","
    output += disruption.getTimeFirstDetected + "\n"
    //    output += (contractRoute + "," + direction + ",\"" + stopA + "\",\"" + stopB + "\"," + disruption.getDelayInMinutes + "," + totalDisruptionTime + ",0," + disruption.getTimeFirstDetected + "\n")
    //    logger.trace("{} - {} disrupted section between stop [{}] and stop [{}] of [{}] minutes. ", Array[Object](route.getContractRoute, Route.getDirectionString(run), stopA, stopB, disruption.getDelayInMinutes.toString))
  }


  //TODO: java.io.FileNotFoundException: E:\Workspace\iBusNetTestDirectory\DisruptionReports\Report.csv (The process cannot access the file because it is being used by another process)
  def save(): Unit = {
    checkOutputDirectory()
    try {
      if (output.length > 0) {
        if (outputFile.exists()) {
          val newFile: File = new File(outputDirectory.getAbsolutePath + "\\Report_" + prevTime + ".csv")
          outputFile.renameTo(newFile)
        }

        val fileWriter = new PrintWriter(new File(outputFilename))
        fileWriter.write("Route,Direction,SectionStart,SectionEnd,DisruptionObserved,RouteTotal,Trend,TimeFirstDetected\n" + output)
        fileWriter.close()
        //TODO: here notify fron ent of change
        prevTime = dateFormat.format(Calendar.getInstance().getTime())
      }
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
