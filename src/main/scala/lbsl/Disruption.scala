package lbsl

import java.sql._
import java.util.Date

import _root_.utility.{DBConnectionPool, Environment}
import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 04/02/2015.
 */
class Disruption(private var sectionStartIndex: Integer,
                 private var sectionEndIndex: Integer,
                 private var sectionStart: String,
                 private var sectionEnd: String,
                 private var delaySeconds: Double,
                 private var totalDelaySeconds: Double,
                 private val timeFirstDetected: Date) {

  private var id: Integer = null
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var trend: Integer = Disruption.TrendWorsening

  def getSectionStartIndex: Integer = sectionStartIndex

  def getSectionEndIndex: Integer = sectionEndIndex

  def getSectionStartBusStop: String = sectionStart

  def getSectionEndBusStop: String = sectionEnd

  def getTotalDelay: Integer = {
    return totalDelaySeconds.toInt
  }

  def getTotalDelayInMinutes: Integer = {
    return getTotalDelay / 60
  }

  def getDelay: Integer = {
    return delaySeconds.toInt
  }

  def getDelayInMinutes: Integer = {
    return getDelay / 60
  }

  def getTimeFirstDetected: Date = timeFirstDetected

  def getTrend: Integer = trend

  def update(newDelaySeconds: Double, newTotalDelaySeconds: Double): Unit = {
    update(sectionStartIndex, sectionEndIndex, sectionStart, sectionEnd, newDelaySeconds, newTotalDelaySeconds)
  }

  def update(newSectionStartIndex: Integer, newSectionEndIndex: Integer, newSectionStart: String, newSectionEnd: String, newDelaySeconds: Double, newTotalDelaySeconds: Double): Unit = {
    //TODO: Consider the section size for the trend as well
    val oldSectionSize = this.sectionEndIndex - this.sectionStartIndex
    this.sectionStartIndex = newSectionStartIndex
    this.sectionEndIndex = newSectionEndIndex
    this.sectionStart = newSectionStart
    this.sectionEnd = newSectionEnd
    this.totalDelaySeconds = newTotalDelaySeconds
    if (newDelaySeconds > delaySeconds) {
      trend = Disruption.TrendWorsening
    } else if (newDelaySeconds < delaySeconds) {
      trend = Disruption.TrendImproving
    } else if (oldSectionSize < this.sectionEndIndex - this.sectionStartIndex) {
      trend = Disruption.TrendWorsening
    } else {
      trend = Disruption.TrendStable
    }
    this.delaySeconds = newDelaySeconds
  }

  //    TODO:Extend this to capture all cases
  def equals(that: Disruption): Boolean = {
    if (this.sectionStartIndex == that.sectionStartIndex ||
      this.sectionEndIndex == that.sectionEndIndex) {
      return true
    } else if (this.sectionStartIndex >= that.sectionStartIndex && this.sectionStartIndex <= that.sectionEndIndex) {
      return true
    } else if (this.sectionEndIndex <= that.sectionEndIndex && this.sectionEndIndex >= that.sectionStartIndex) {
      return true
    }
    return false
  }

  def clear(date: Date): Unit = {
    updateDBEntry(date)
  }

  def save(route: String, run: Integer): Unit = {
    if (id == null) {
      id = Disruption.getNextId()
      newEntry(route, run)
    } else {
      updateDBEntry()
    }
  }

  private def updateDBEntry(clearedAt: Date = null): Unit = {
    var preparedStatement: PreparedStatement = null
    val query = "UPDATE \"Disruptions\" SET \"fromStopLBSLCode\" = ?, \"toStopLBSLCode\" = ?, \"delayInSeconds\" = ?, trend = ?,  \"routeTotalDelayInSeconds\" = ?, \"clearedAt\" = ? WHERE id = ? ;"
    try {
      preparedStatement = Environment.getDBTransaction.connection.prepareStatement(query)
      preparedStatement.setString(1, sectionStart)
      preparedStatement.setString(2, sectionEnd)
      preparedStatement.setDouble(3, delaySeconds)
      preparedStatement.setInt(4, trend)
      preparedStatement.setDouble(5, totalDelaySeconds)
      if (clearedAt != null) {
        preparedStatement.setTimestamp(6, new Timestamp(clearedAt.getTime))
      } else {
        preparedStatement.setNull(6, Types.NULL)
      }
      preparedStatement.setInt(7, id)
      preparedStatement.executeUpdate()
    }
    catch {
      case e: SQLException => logger.error("Exception: with query ({}) ", preparedStatement.toString, e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
  }

  private def newEntry(route: String, run: Integer): Unit = {
    if (delaySeconds > 2400 || totalDelaySeconds > 2400) {
      logger.debug("New disruption added on route {} run {}with delay {}mins and total delay {}mins.", scala.Array[Object](route, run.toString, (delaySeconds / 60).toString, (totalDelaySeconds / 60).toString))
    }
    var preparedStatement: PreparedStatement = null
    val query = "INSERT INTO \"Disruptions\" (id, \"fromStopLBSLCode\", \"toStopLBSLCode\", route, run, \"delayInSeconds\", \"firstDetectedAt\", trend, \"routeTotalDelayInSeconds\") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"
    try {
      preparedStatement = Environment.getDBTransaction.connection.prepareStatement(query)
      preparedStatement.setInt(1, id)
      preparedStatement.setString(2, sectionStart)
      preparedStatement.setString(3, sectionEnd)
      preparedStatement.setString(4, route)
      preparedStatement.setInt(5, run)
      preparedStatement.setDouble(6, delaySeconds)
      preparedStatement.setTimestamp(7, new Timestamp(timeFirstDetected.getTime))
      preparedStatement.setInt(8, trend)
      preparedStatement.setDouble(9, totalDelaySeconds)
      preparedStatement.executeUpdate()
    }
    catch {
      case e: SQLException => logger.error("Exception: with query ({}) ", preparedStatement.toString, e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
  }
}

object Disruption {

  private var id: Integer = null
  //  NRT delay classifications:
  //  Moderate - 0 - 20 min
  //  Serious - 21 - 40 min
  //  Severe - 41 - 60 min

  //  final val SectionModerate: Integer = 10
  //  final val SectionSerious: Integer = 20
  //  final val SectionSevere: Integer = 40
  //
  //  final val RouteSerious: Integer = 30
  //  //40
  //  final val RouteSevere: Integer = 50 //60

  final val TrendImproving = 1
  final val TrendStable = 0
  final val TrendWorsening = -1


  def getNextId(): Integer = {
    this.synchronized {
      if (id == null) {
        getMaxId()
      }
      id += 1
      return id
    }
  }

  private def getMaxId() {
    var connection: Connection = null
    var statement: Statement = null
    try {
      connection = DBConnectionPool.getConnection()
      statement = connection.createStatement()
      val rs = statement.executeQuery("SELECT max(id) FROM \"Disruptions\"")
      while (rs.next()) {
        id = rs.getInt(1)
      }
    }
    catch {
      case e: SQLException => LoggerFactory.getLogger(getClass().getSimpleName).error("Exception:", e)
    } finally {
      if (statement != null) {
        statement.close()
      }
      if (connection != null) {
        DBConnectionPool.returnConnection(connection)
      }
    }
  }
}
