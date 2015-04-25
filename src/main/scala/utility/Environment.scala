package utility

import java.io.File
import java.sql.{Connection, PreparedStatement, SQLException}
import java.text.SimpleDateFormat
import java.util.Date

import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.utility.DBTransaction

/**
 * Created by Konstantin on 22/03/2015.
 *
 * Static class representing the Environment variables
 * and configurations. Which are all loaded from the database.
 */
object Environment {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val expectedParams = Array[String](
    "dateFormat",
    "disruptionRouteSeriousThresholdSeconds",
    "disruptionRouteSevereThresholdSeconds",
    "disruptionSectionMediumThresholdSeconds",
    "disruptionSectionMinThresholdSeconds",
    "disruptionSectionSeriousThresholdSeconds",
    "disruptionSectionSevereThresholdSeconds",
    "feedDirectory",
    "feedFileDelimiter",
    "feedFileHeader",
    "feedFilePrefix",
    "feedFileSuffix",
    "monitorThreadSleepIntervalMilliSeconds",
    "processedDirectory",
    "dataValidityTimeInMinutes",
    "movingAverageWindowSize",
    "systemMonitor"
  )

  private var latestFeedTimeOfData: Date = new Date(0)
  private val paramsMap = new mutable.HashMap[String, String]()
  private val quoteRegex: String = "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)"
  private val dbTransaction: DBTransaction = new DBTransaction

  def isSystemMonitorActive(): Boolean = getValue("systemMonitor").toBoolean

  def getMovingAverageWindowSize(): Integer = {
    return Integer.parseInt(getValue("movingAverageWindowSize"))
  }

  def getDBTransaction = dbTransaction

  def getLatestFeedTimeOfData = latestFeedTimeOfData

  def setLatestFeedTimeOfData(date: Date): Unit = {
    latestFeedTimeOfData = date
  }

  def getFeedDirectory(): File = {
    val feedDirectory = new File(getValue("feedDirectory"))
    if (!feedDirectory.exists()) {
      logger.warn("Processed directory missing. Trying to create directory [{}].", feedDirectory.getAbsolutePath)
      if (!feedDirectory.mkdir()) {
        logger.error("Failed to create directory [{}].Terminating application.", feedDirectory.getAbsolutePath)
        System.exit(1)
      }
    }
    return feedDirectory
  }

  def getFeedFilePrefix: String = getValue("feedFilePrefix")

  def getFeedFileSuffix: String = getValue("feedFileSuffix")

  def getFeedFileDelimiter: String = getValue("feedFileDelimiter")

  def getFeedFileRegex: String = getFeedFileDelimiter + quoteRegex

  def getFeedFileHeader: Boolean = {
    getValue("feedFileHeader").toBoolean
  }

  def getDataValidityTimeInMinutes: Integer = {
    return Integer.parseInt(getValue("dataValidityTimeInMinutes"))
  }

  def getProcessedDirectory(): File = {
    val processedDirectory = new File(getValue("processedDirectory"))
    if (!processedDirectory.exists()) {
      logger.warn("Processed directory missing. Trying to create directory [{}].", processedDirectory.getAbsolutePath)
      if (!processedDirectory.mkdir()) {
        logger.error("Failed to create directory [{}].Terminating application.", processedDirectory.getAbsolutePath)
        System.exit(1)
      }
    }
    return processedDirectory
  }

  def getMonitorThreadSleepInterval(): Long = {
    return getValue("monitorThreadSleepIntervalMilliSeconds").toLong
  }

  def getSectionMediumThreshold: Integer = {
    return Integer.parseInt(getValue("disruptionSectionMediumThresholdSeconds"))
  }

  def getSectionSeriousThreshold: Integer = {
    return Integer.parseInt(getValue("disruptionSectionSeriousThresholdSeconds"))
  }

  def getSectionSevereThreshold: Integer = {
    return Integer.parseInt(getValue("disruptionSectionSevereThresholdSeconds"))
  }

  def getSectionMinThreshold: Integer = {
    return Integer.parseInt(getValue("disruptionSectionMinThresholdSeconds"))
  }

  def getRouteSevereThreshold: Integer = {
    return Integer.parseInt(getValue("disruptionRouteSevereThresholdSeconds"))
  }

  def getRouteSeriousThreshold(): Integer = {
    return Integer.parseInt(getValue("disruptionRouteSeriousThresholdSeconds"))
  }

  def getDateFormat(): SimpleDateFormat = {
    return new SimpleDateFormat(getValue("dateFormat"))
  }

  def init(): Unit = {
    loadParams()
    checkParams()
  }

  def update(): Unit = {
    loadParams()
    checkParams()
  }

  private def getValue(key: String): String = {
    return paramsMap.getOrElse(key, null)
  }

  private def loadParams(): Unit = {
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    val query = "SELECT key, value FROM \"EngineConfigurations\""
    try {
      connection = DBConnectionPool.getConnection()
      preparedStatement = connection.prepareStatement(query)
      val rs = preparedStatement.executeQuery()
      while (rs.next()) {
        paramsMap.put(rs.getString("key"), rs.getString("value"))
      }
    }
    catch {
      case e: SQLException => logger.error("Exception:", e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
      if (connection != null) {
        DBConnectionPool.returnConnection(connection)
      }
    }
  }

  private def checkParams(): Unit = {
    for (key <- expectedParams) {
      if (paramsMap.get(key) == None) {
        logger.warn("Missing configuration [{}] parameter. Terminating application.", key)
        System.exit(-1)
      }
      if (paramsMap.get(key) == null) {
        logger.warn("Missing configuration value for [{}]. Terminating application.", key)
        System.exit(-1)
      }
    }
  }

  def test(): Unit = {
    logger.trace("MonitorThreadSleepInterval - [{}]", getMonitorThreadSleepInterval().toString)
    logger.trace("FeedsDirectory - [{}]", getFeedDirectory().getAbsolutePath)
    logger.trace("ProcessedDirectory - [{}]", getProcessedDirectory().getAbsolutePath)
    logger.trace("DateFormat - [{}]", getDateFormat().toPattern)
    logger.trace("FeedFilePrefix - [{}]", getFeedFilePrefix)
    logger.trace("FeedFileSuffix - [{}]", getFeedFileSuffix)
    logger.trace("FeedFileDelimiter - [{}]", getFeedFileDelimiter)
    logger.trace("FeedFileHeader - [{}]", getFeedFileHeader.toString)
    logger.trace("FeedFileRegex - [{}]", getFeedFileRegex)
    logger.trace("SectionMediumThreshold - [{}]", getSectionMediumThreshold.toString)
    logger.trace("SectionSeriousThreshold - [{}]", getSectionSeriousThreshold.toString)
    logger.trace("SectionSevereThreshold - [{}]", getSectionSevereThreshold.toString)
    logger.trace("RouteSeriousThreshold - [{}]", getRouteSeriousThreshold.toString)
    logger.trace("RouteSevereThreshold - [{}]", getRouteSevereThreshold.toString)
    logger.trace("SectionMinThreshold - [{}]", getSectionMinThreshold.toString)
    logger.trace("Data validity in minutes - [{}]", getDataValidityTimeInMinutes.toString)
    logger.trace("Moving Average Window Size - [{}]", getMovingAverageWindowSize.toString)
    logger.trace("System Monitor - [{}]", isSystemMonitorActive.toString)
  }
}
