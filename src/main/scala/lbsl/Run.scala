package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}

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

  private var sections: Array[Section] = null

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
  }

  def detectDisruptions(): Unit = {
    disruptions.copyToBuffer(prevDisruptions)
    disruptions.clear()
    if (getTotalDelay() >= Environment.getSectionMediumThreshold) {
      findDisruptedSections(Environment.getSectionMinThreshold)
      if (disruptions.isEmpty && getTotalDelay > Environment.getRouteSeriousThreshold) {
        findDisruptedSections(Environment.getSectionMinThreshold / 2)
      }
      //TODO: BEGIN Remove - just for testing purposes
      if (disruptions.isEmpty && getTotalDelay > Environment.getRouteSeriousThreshold) {
        var max: Double = 0
        for (section <- sections) {
          if (section.getDelay() > max) {
            max = section.getDelay()
          }
        }
        logger.debug("Route {} direction {} disrupted by {} minutes [max section disruption is {}].",
          Array[Object](routeNumber, Run.getRunString(run), (getTotalDelay / 60).toString, max.toString))
      }
      //TODO: END
    }
    updateDB()
  }

  private def updateDB(): Unit = {
    //TODO: save sections to DB as well
    for (disruption <- prevDisruptions) {
      disruption.clear()
    }
    for (disruption <- disruptions) {
      disruption.save()
    }
  }

  private def findDisruptedSections(sectionMinThreshold: Integer): Unit = {
    var sectionStartStopIndex: Integer = null
    var disruptionSeconds: Double = 0
    for (i <- 0 until sections.length) {
      if (sections(i).getDelay() > sectionMinThreshold) {
        if (sectionStartStopIndex == null) {
          sectionStartStopIndex = i
        }
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
  }

  private def addDisruption(sectionStartStopIndex: Integer, sectionEndStopIndex: Integer, delaySeconds: Double): Unit = {
    var disruption = new Disruption(sectionStartStopIndex, sectionEndStopIndex, busStops(sectionStartStopIndex), busStops(sectionEndStopIndex), delaySeconds)
    val index = prevDisruptions.indexWhere(disruption.equals(_))
    if (index > -1) {
      disruption = prevDisruptions.remove(index)
      disruption.update(sectionStartStopIndex, sectionEndStopIndex, busStops(sectionStartStopIndex), busStops(sectionEndStopIndex), delaySeconds)
    }
    disruptions.append(disruption)
  }

  def checkStops(prevObservation: Observation, observation: Observation, lostTime: Double): Boolean = {
    val prevLastStopIndex = busStops.indexOf(prevObservation.getLastStopShortDesc)
    val lastStopIndex = busStops.indexOf(observation.getLastStopShortDesc)
    //TODO: Consider cases where the lastSTopIndex is the last stop from the given run
    if (lastStopIndex >= prevLastStopIndex && prevLastStopIndex > -1 && lastStopIndex < busStops.size - 1) {
      val numberOfSections = (prevLastStopIndex - lastStopIndex) + 1
      val lostTimePerSection = lostTime / numberOfSections
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

  def getTotalDelay(): Double = {
    var totalDelay: Double = 0
    for (section <- sections) {
      totalDelay += section.getDelay()
    }
    return totalDelay
  }

  private def generateSections(): Unit = {
    sections = new Array[Section](busStops.size - 1)
    for (i <- 1 until busStops.size) {
      sections(i - 1) = new Section(busStops(i - 1), busStops(i))
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