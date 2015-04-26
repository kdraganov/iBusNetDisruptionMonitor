package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}
import java.util.Date

import org.slf4j.LoggerFactory
import utility.{DBConnectionPool, Environment}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Konstantin on 22/03/2015.
 *
 * Class representing a single run on a given route.
 */
class Run(private val routeNumber: String, private val run: Integer) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStops: ArrayBuffer[String] = new ArrayBuffer[String]()
  private val disruptions: ArrayBuffer[Disruption] = new ArrayBuffer[Disruption]()
  private val prevDisruptions: ArrayBuffer[Disruption] = new ArrayBuffer[Disruption]()
  private val sections: ArrayBuffer[Section] = new ArrayBuffer[Section]()

  /**
   * Method which initialises the Run by
   * loading all information from the database
   * and generating and initialising its sections.
   */
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

  /**
   * Method for detecting any disruptions along the run.
   */
  def detectDisruptions(): Unit = {
    prevDisruptions.clear()
    disruptions.copyToBuffer(prevDisruptions)
    disruptions.clear()
    findDisruptions()
    updateDB()
  }

  private def updateDB(): Unit = {
    val latestObservationDate: Date = Environment.getLatestFeedTimeOfData
    for (disruption <- prevDisruptions) {
      disruption.update(getLostTime(disruption.getSectionStartIndex, disruption.getSectionEndIndex), getCumulativeLostTime())
      disruption.clear(latestObservationDate)
    }

    for (disruption <- disruptions) {
      disruption.save(routeNumber, run)
    }

    if (!disruptions.isEmpty || !prevDisruptions.isEmpty) {
      for (section <- sections) {
        section.save(latestObservationDate)
      }
    }
  }

  private def findDisruptions(): Unit = {
    val cumulativeLostTime = getCumulativeLostTime()
    if (cumulativeLostTime >= Environment.getSectionMediumThreshold) {
      findDisruptedSections(Environment.getSectionMinThreshold)

      if (disruptions.isEmpty && cumulativeLostTime > Environment.getRouteSeriousThreshold) {
        findDisruptedSections(Environment.getSectionMinThreshold / 2)
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
  }

  /**
   * Method which checks the run for any disrupted sections.
   * @param sectionMinThreshold Integer - the minimum section time loss threshold
   */
  private def findDisruptedSections(sectionMinThreshold: Integer): Unit = {
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
            addDisruption(sectionStartStopIndex, i, disruptionSeconds)
          }
          sectionStartStopIndex = null
          disruptionSeconds = 0
        }
      }

    }
    if (sectionStartStopIndex != null) {
      // end of sectionDisruption
      if (disruptionSeconds >= Environment.getSectionMediumThreshold) {
        addDisruption(sectionStartStopIndex, sections.length - 1, disruptionSeconds)
      }
    }
  }

  private def addDisruption(sectionStartStopIndex: Integer, sectionEndStopIndex: Integer, delaySeconds: Double): Unit = {
    var disruption = new Disruption(sectionStartStopIndex, sectionEndStopIndex, busStops(sectionStartStopIndex), busStops(sectionEndStopIndex), delaySeconds, getCumulativeLostTime(), Environment.getLatestFeedTimeOfData)
    val index = prevDisruptions.indexWhere(disruption.equals(_))
    if (index > -1) {
      disruption = prevDisruptions.remove(index)
      disruption.update(sectionStartStopIndex, sectionEndStopIndex, busStops(sectionStartStopIndex), busStops(sectionEndStopIndex), delaySeconds, getCumulativeLostTime())
    }
    disruptions.append(disruption)
  }

  /**
   *
   * @param prevObservation Observation - the previous observation
   * @param observation Observation - the subsequent observation
   * @return Boolean - tru if the observation are made from the same run of a given route
   */
  def checkStops(prevObservation: Observation, observation: Observation): Boolean = {
    //This controls whether to include or exclude curtailments (in case when included we assume that the lost time has happened on previous trip)
    if (prevObservation.getTripId == observation.getTripId || (prevObservation.getTripType == 3 && observation.getTripType == 2)) {
      val prevLastStopIndex = busStops.indexOf(prevObservation.getLastStopShortDesc)
      val lastStopIndex = busStops.indexOf(observation.getLastStopShortDesc)
      if (lastStopIndex >= prevLastStopIndex && prevLastStopIndex > -1 && lastStopIndex <= busStops.size - 1) {
        val numberOfSections = (lastStopIndex - prevLastStopIndex) + 1
        val lostTimePerSection = (observation.getScheduleDeviation - prevObservation.getScheduleDeviation) / numberOfSections
        for (i <- prevLastStopIndex to Math.min(lastStopIndex, sections.size - 1)) {
          sections(i).addObservation(observation.getVehicleId, lostTimePerSection, observation.getTimeOfData)
          //          sections(i).addObservation(new Tuple2(lostTimePerSection, observation.getTimeOfData))
        }
        return true
      }
    }
    return false
  }

  /**
   * Method for clearing the data stored for each section.
   */
  def clearSections(): Unit = {
    for (section <- sections) {
      section.clear()
    }
  }

  private def getLostTime(start: Int, end: Int): Double = {
    var totalDelay: Double = 0
    for (i <- start until end) {
      totalDelay += Math.max(sections(i).getDelay(), 0)
    }
    return totalDelay
  }

  private def getCumulativeLostTime(considerNegatives: Boolean = false): Double = {
    var totalDelay: Double = 0
    for (section <- sections) {
      if (considerNegatives) {
        totalDelay += section.getDelay()
      } else {
        totalDelay += Math.max(section.getDelay(), 0)
      }
    }
    return totalDelay
  }

  private def generateSections(): Unit = {
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
  /**
   * Static method which returns the string for a run
   * @param run Integer - the intger representing the run type
   * @return String - the string for the provided run
   */
  def getRunString(run: Int): String = {
    run match {
      case 1 => return "Outbound"
      case 2 => return "Inbound"
      case default => return "Undefined"
    }
  }
}