package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}
import java.util.Date

import org.slf4j.LoggerFactory
import utility.{DBConnectionPool, Environment}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Konstantin on 22/03/2015.
 */
class Run(private val routeNumber: String, private val run: Integer) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStops: ArrayBuffer[String] = new ArrayBuffer[String]()
  private val disruptions: ArrayBuffer[Disruption] = new ArrayBuffer[Disruption]()

  private val prevDisruptions: ArrayBuffer[Disruption] = new ArrayBuffer[Disruption]()

  private var sections: ArrayBuffer[Section] = null

  //  private var latestObservationTime: Date = null

  def init(): Unit = {
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    val query = "SELECT \"busStopLBSLCode\" FROM \"BusRouteSequences\" WHERE route = ? AND run = ? ORDER BY \"sequence\""
    try {
      connection = DBConnectionPool.getConnection()
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setString(1, routeNumber)
      preparedStatement.setInt(2, run)
      val rs = preparedStatement.executeQuery()
      while (rs.next()) {
        busStops.append(rs.getString("busStopLBSLCode"))
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
    generateSections()

    if (busStops.length - sections.length != 1) {
      logger.debug("ERROR: Number of bus stop {} and number of sections {}.", busStops.length, sections.length)
      logger.debug("Terminating application.")
      System.exit(1)
    }
  }

  def detectDisruptions(): Unit = {
    prevDisruptions.clear()
    disruptions.copyToBuffer(prevDisruptions)
    disruptions.clear()
    val cumulativeLostTime = getCumulativeLostTime()
    if (cumulativeLostTime >= Environment.getSectionMediumThreshold) {
      findDisruptedSections(Environment.getSectionMinThreshold, cumulativeLostTime)

      if (disruptions.isEmpty && cumulativeLostTime > Environment.getRouteSeriousThreshold) {
        findDisruptedSections(Environment.getSectionMinThreshold / 2, cumulativeLostTime)
      }
      if (disruptions.isEmpty && cumulativeLostTime > Environment.getRouteSeriousThreshold) {
        findDisruptedSections(90, cumulativeLostTime)
      }

      //TODO: BEGIN Remove - just for testing purposes
      if (disruptions.isEmpty && cumulativeLostTime > Environment.getRouteSeriousThreshold) {
        var max: Double = 0
        for (section <- sections) {
          if (section.getDelay() > max) {
            max = section.getDelay()
          }
        }
        logger.debug("Route {} direction {} disrupted by {} minutes [max section disruption is {}].",
          Array[Object](routeNumber, Run.getRunString(run), (cumulativeLostTime / 60).toString, max.toString))
      }
      //TODO: END

    }
    updateDB()
  }

  private def updateDB(): Unit = {
    if (!prevDisruptions.isEmpty) {
      var clearedAt: Date = null
      for (section <- sections if section.getLatestObservationTime() != null) {
        if (clearedAt == null || section.getLatestObservationTime().after(clearedAt)) {
          clearedAt = section.getLatestObservationTime()
        }
      }
      for (disruption <- prevDisruptions) {
        disruption.clear(clearedAt)
      }
    }

    for (disruption <- disruptions) {
      disruption.save(routeNumber, run)
    }
    if (!disruptions.isEmpty) {
      for (section <- sections) {
        section.save()
      }
    }
  }

  private def findDisruptedSections(sectionMinThreshold: Integer, totalDelay: Double): Unit = {
    var sectionStartStopIndex: Integer = null
    var disruptionSeconds: Double = 0
    for (i <- 0 until sections.length) {

      if (sections(i).getDelay() > sectionMinThreshold) {
        if (sectionStartStopIndex == null) {
          sectionStartStopIndex = i
        }
        disruptionSeconds += sections(i).getDelay()
      } else {
        if (sectionStartStopIndex != null) {
          // end of sectionDisruption
          if (disruptionSeconds >= Environment.getSectionMediumThreshold) {
            addDisruption(sectionStartStopIndex, i, disruptionSeconds, totalDelay, sections(i - 1).getLatestObservationTime())
          }
          sectionStartStopIndex = null
          disruptionSeconds = 0
        }
      }

    }
  }

  private def addDisruption(sectionStartStopIndex: Integer, sectionEndStopIndex: Integer, delaySeconds: Double, totalDelaySeconds: Double, detectedAt: Date): Unit = {
    var disruption = new Disruption(sectionStartStopIndex, sectionEndStopIndex, busStops(sectionStartStopIndex), busStops(sectionEndStopIndex), delaySeconds, totalDelaySeconds, detectedAt)
    val index = prevDisruptions.indexWhere(disruption.equals(_))
    if (index > -1) {
      disruption = prevDisruptions.remove(index)
      disruption.update(sectionStartStopIndex, sectionEndStopIndex, busStops(sectionStartStopIndex), busStops(sectionEndStopIndex), delaySeconds, totalDelaySeconds)
    }
    disruptions.append(disruption)
  }

  def checkStops(prevObservation: Observation, observation: Observation): Boolean = {
    val prevLastStopIndex = busStops.indexOf(prevObservation.getLastStopShortDesc)
    val lastStopIndex = busStops.indexOf(observation.getLastStopShortDesc)
    //TODO: Consider cases where the lastSTopIndex is the last stop from the given run
    if (lastStopIndex >= prevLastStopIndex && prevLastStopIndex > -1 && lastStopIndex < busStops.size - 1) {
      val numberOfSections = (lastStopIndex - prevLastStopIndex) + 1
      val lostTimePerSection = (observation.getScheduleDeviation - prevObservation.getScheduleDeviation) / numberOfSections
      for (i <- prevLastStopIndex to lastStopIndex) {
        sections(i).addObservation(new Tuple2(lostTimePerSection, observation.getTimeOfData))
      }
      return true
    }
    return false
  }

  def clearSections(): Unit = {
    for (section <- sections) {
      section.clear()
    }
  }

  def getCumulativeLostTime(): Double = {
    var totalDelay: Double = 0
    for (section <- sections) {
      totalDelay += Math.max(section.getDelay(), 0)
    }
    return totalDelay
  }

  private def generateSections(): Unit = {
    sections = new ArrayBuffer[Section]()
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    val query = "SELECT id, \"startStopLBSLCode\", \"endStopLBSLCode\", \"sequence\" FROM \"Sections\" WHERE route = ? AND run = ? ORDER BY \"sequence\""
    try {
      connection = DBConnectionPool.getConnection()
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setString(1, routeNumber)
      preparedStatement.setInt(2, run)
      val rs = preparedStatement.executeQuery()
      while (rs.next()) {
        sections.append(new Section(rs.getInt("id"), rs.getInt("sequence"), rs.getString("startStopLBSLCode"), rs.getString("endStopLBSLCode")))
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
}

object Run {

  def getRunString(run: Int): String = {
    run match {
      case 1 => return "Outbound"
      case 2 => return "Inbound"
      case default => return "Undefined"
    }

  }
}