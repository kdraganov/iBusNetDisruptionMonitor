package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}

import org.slf4j.LoggerFactory
import utility.{DBConnectionPool, Environment}

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.concurrent.duration._

/**
 * Created by Konstantin on 26/01/2015.
 */

class Route(private val contractRoute: String) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val runs: ArrayBuffer[Run] = new ArrayBuffer[Run]()
  private val busesOnRoute: HashMap[Integer, ArrayBuffer[Observation]] = new HashMap[Integer, ArrayBuffer[Observation]]()

  def init(): Unit = {
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    val query = "SELECT DISTINCT run FROM \"BusRouteSequences\" WHERE run <= 2 AND route = ? ORDER BY run "
    try {
      connection = DBConnectionPool.getConnection()
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setString(1, contractRoute)
      val rs = preparedStatement.executeQuery()
      while (rs.next()) {
        runs.append(new Run(contractRoute, rs.getInt("run")))
      }
    }
    catch {
      case e: SQLException => logger.error("Exception: with query ({}) ", preparedStatement.toString, e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
      if (connection != null) {
        DBConnectionPool.returnConnection(connection)
      }
    }
    for (run <- runs) {
      run.init()
    }
  }

  override
  def run(): Unit = {
    updateObservations()
    for (run <- runs) {
      run.clearSections()
    }
    for ((busId, observationList) <- busesOnRoute if observationList.size > 1) {
      //       logger.trace("Route {} observation list size = {} ", getContractRoute, observationList.length)
      for (i <- 1 until observationList.size) {
        assignLostTimeToSections(observationList(i - 1), observationList(i))
      }
    }
    for (run <- runs) {
      run.detectDisruptions()
    }
  }

  private def assignLostTimeToSections(prevObservation: Observation, observation: Observation): Boolean = {
    val scheduleDeviationDifference = observation.getScheduleDeviation - prevObservation.getScheduleDeviation
    if (scheduleDeviationDifference > 0) {
      for (run <- runs) {
        if (run.checkStops(prevObservation, observation, scheduleDeviationDifference)) {
          return true
        }
      }
    }
    return false
  }

  /**
   *
   * @return boolean true if there are active (e.g. have received reading in the past 1h or so) buses on the route
   *         false otherwise
   */
  def isRouteActive(): Boolean = {
    updateObservations()
    !busesOnRoute.isEmpty
  }

  def addObservation(observation: Observation): Unit = {
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ArrayBuffer[Observation]())
    observationList.append(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }


  def getContractRoute = contractRoute


  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  private def updateObservations(): Unit = {
    for ((busId, observationList) <- busesOnRoute) {
      val sortedObservationList = observationList.sortBy(x => x.getTimeOfData)
      // difference in MILLISECONDS
      var timeDiff = Duration(Environment.getLatestFeedTime - sortedObservationList(0).getTimeOfData.getTime, MILLISECONDS)
      while (timeDiff.toMinutes > Environment.getDataValidityTimeInMinutes && sortedObservationList.size > 0) {
        timeDiff = Duration(Environment.getLatestFeedTime - sortedObservationList.remove(0).getTimeOfData.getTime, MILLISECONDS)
      }
      if (sortedObservationList.isEmpty) {
        //logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      } else {
        busesOnRoute.put(busId, sortedObservationList)
      }
    }
  }

}
