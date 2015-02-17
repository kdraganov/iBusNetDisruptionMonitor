package utility


import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import org.slf4j.LoggerFactory

import scala.xml._

/**
 * Created by Konstantin on 04/02/2015.
 */
object Configuration {

  private var configFile: File = null
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var title: String = null
  private var mode: String = null
  private var monitorThreadSleepInterval: Long = 0
  private var feedsDirectory: File = null
  private var processedDirectory: File = null
  private var dateFormat: SimpleDateFormat = null
  private var busStopFile: File = null
  private var busStopFileDelimiter: String = null
  private var busStopFileHeader: Boolean = false
  private var busRouteFile: File = null
  private var busRouteFileDelimiter: String = null
  private var busRouteFileHeader: Boolean = false
  private var feedFileStartWith: String = null
  private var feedFileEndWith: String = null
  private var feedFileDelimiter: String = null
  private var feedFileHeader: Boolean = false
  private var feedUpdateInterval: Integer = null
  private var latestFeedTime: Date = new Date(0)

  private val quoteRegex: String = "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)"
  private final val DataValidityTimeInHours: Integer = 2

  def getLatestFeedTime = latestFeedTime.getTime

  def getLatestFeedDateTime = latestFeedTime.getTime

  def setLatestFeedDateTime(date: Date): Unit = {
    latestFeedTime = date
  }

  def getDataValidityTimeInHours = DataValidityTimeInHours

  def getTitle(): String = title

  def getMode(): String = mode

  def getMonitorThreadSleepInterval(): Long = monitorThreadSleepInterval

  def getFeedsDirectory(): File = feedsDirectory

  def getProcessedDirectory(): File = {
    if (!processedDirectory.exists()) {
      logger.warn("Processed directory missing. Trying to create directory [{}].", processedDirectory.getAbsolutePath)
      if (!processedDirectory.mkdir()) {
        logger.error("Failed to create directory [{}].Terminating application.", processedDirectory.getAbsolutePath)
        System.exit(1)
      }
    }
    return processedDirectory
  }

  def getDateFormat(): SimpleDateFormat = dateFormat

  def getBusStopFile(): File = busStopFile

  def getBusStopFileDelimiter: String = busStopFileDelimiter

  def getBusStopFileRegex: String = busStopFileDelimiter + quoteRegex

  def getBusStopFileHeader: Boolean = busStopFileHeader

  def getBusRouteFile: File = busRouteFile

  def getBusRouteFileDelimiter: String = busRouteFileDelimiter

  def getBusRouteFileRegex: String = busRouteFileDelimiter + quoteRegex

  def getBusRouteFileHeader: Boolean = busRouteFileHeader

  def getFeedFileStartWith: String = feedFileStartWith

  def getFeedFileEndWith: String = feedFileEndWith

  def getFeedFileDelimiter: String = feedFileDelimiter

  def getFeedFileRegex: String = feedFileDelimiter + quoteRegex

  def getFeedFileHeader: Boolean = feedFileHeader

  def getFeedUpdateInterval: Integer = feedUpdateInterval

  def getRouteSegmentSize: Integer = {
    return Math.round(feedUpdateInterval / 60)
  }

  def test(): Unit = {
    logger.trace(getTitle())
    logger.trace(getMode())
    logger.trace(getMonitorThreadSleepInterval().toString)
    logger.trace(getFeedsDirectory().getAbsolutePath)
    logger.trace(getProcessedDirectory().getAbsolutePath)
    logger.trace(getDateFormat().toString)
    logger.trace(getBusStopFile().getAbsolutePath)
    logger.trace(getBusStopFileDelimiter)
    logger.trace(getBusStopFileHeader.toString)
    logger.trace(getBusRouteFile.toString)
    logger.trace(getBusRouteFileDelimiter)
    logger.trace(getBusRouteFileHeader.toString)
    logger.trace(getFeedFileStartWith)
    logger.trace(getFeedFileEndWith)
    logger.trace(getFeedFileDelimiter)
    logger.trace(getFeedFileHeader.toString)
    logger.trace(feedUpdateInterval.toString)
  }

  def setConfigurationFilePath(configurationFilePath: String): Unit = {
    configFile = new File(configurationFilePath)
    if (!configFile.exists()) {
      logger.error("Cannot find specified configuration file [{}].", configFile.getAbsolutePath)
    }
  }

  def init(): Unit = {
    val settingsXML = XML.loadFile(configFile)
    title = (settingsXML \\ "title").text
    mode = (settingsXML \\ "mode").text
    monitorThreadSleepInterval = (settingsXML \\ "monitorThreadSleepInterval").text.toLong
    setFeedsDirectories(settingsXML)
    dateFormat = new SimpleDateFormat((settingsXML \\ "dateFormat").text)
    setBuStopFile(settingsXML)
    setBuRouteFile(settingsXML)
    setFeedFile(settingsXML)
  }

  def update(): Unit = {
    init()
  }

  private def setFeedFile(settingsXML: Elem): Unit = {
    feedFileStartWith = (settingsXML \\ "feedFile" \\ "nameStartWith").text
    feedFileEndWith = (settingsXML \\ "feedFile" \\ "nameEndWith").text
    feedFileDelimiter = (settingsXML \\ "feedFile" \\ "delimiter").text
    feedFileHeader = (settingsXML \\ "feedFile" \\ "header").text.toBoolean
    feedUpdateInterval = (settingsXML \\ "feedFile" \\ "updateInterval").text.toInt
  }

  private def setBuRouteFile(settingsXML: Elem): Unit = {
    busRouteFile = new File((settingsXML \\ "routesFile" \\ "path").text)
    if (!busRouteFile.exists()) {
      logger.error("Specified bus route file [{}] missing. Terminating application.", busRouteFile.getAbsolutePath)
      System.exit(1)
    }
    busRouteFileDelimiter = (settingsXML \\ "routesFile" \\ "delimiter").text
    busRouteFileHeader = (settingsXML \\ "routesFile" \\ "header").text.toBoolean
  }

  private def setBuStopFile(settingsXML: Elem): Unit = {
    busStopFile = new File((settingsXML \\ "busStopFile" \\ "path").text)
    if (!busStopFile.exists()) {
      logger.error("Specified bus stop file [{}] missing. Terminating application.", busStopFile.getAbsolutePath)
      System.exit(1)
    }
    busStopFileDelimiter = (settingsXML \\ "busStopFile" \\ "delimiter").text
    busStopFileHeader = (settingsXML \\ "busStopFile" \\ "header").text.toBoolean
  }

  private def setFeedsDirectories(settingsXML: Elem) {
    feedsDirectory = new File((settingsXML \\ "feedsDir").text)
    if (!feedsDirectory.exists()) {
      logger.error("Specified directory for monitoring [{}] cannot be found. Terminating application.", feedsDirectory.getAbsolutePath)
      println("Error: Feeds directory missing")
      System.exit(1)
    }
    processedDirectory = new File((settingsXML \\ "processedDirectory").text)
    if (!processedDirectory.exists()) {
      logger.warn("Processed directory missing. Trying to create directory [{}].", processedDirectory.getAbsolutePath)
      if (!processedDirectory.mkdir()) {
        logger.error("Failed to create directory [{}].Terminating application.", processedDirectory.getAbsolutePath)
        System.exit(1)
      }
    }
  }
}
