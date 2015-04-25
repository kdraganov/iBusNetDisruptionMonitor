package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}

import org.slf4j.LoggerFactory
import utility.{DBConnectionPool, Environment}

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.concurrent.duration._

/**
 * Created by Konstantin on 26/01/2015.
 *
 * Class representing a route in the bus network.
 */

class Route(private val contractRoute: String) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val runList: ArrayBuffer[Run] = new ArrayBuffer[Run]()
  private val busesOnRoute: HashMap[Integer, ArrayBuffer[Observation]] = new HashMap[Integer, ArrayBuffer[Observation]]()

  def getContractRoute = contractRoute

  /**
   *
   * @return boolean true if there are active (e.g. have received reading in the past 1h or so) buses on the route
   *         false otherwise
   */
  def isActive(): Boolean = {
    updateObservations()
    !busesOnRoute.isEmpty
  }

  /**
   *
   * @param observation Observation - to be added to the route
   */
  def addObservation(observation: Observation): Unit = {
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ArrayBuffer[Observation]())
    observationList.append(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }

  /**
   * Method for initialisation of the route. This includes
   * the preload of all the information for the route and its
   * runs and sections from the database.
   */
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
        runList.append(new Run(contractRoute, rs.getInt("run")))
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
    for (run <- runList) {
      run.init()
    }
  }

  override
  def run(): Unit = {
    updateObservations()
    for ((busId, observationList) <- busesOnRoute if observationList.size > 1) {
      for (i <- 1 until observationList.size) {
        assignLostTimeToSections(observationList(i - 1), observationList(i))
      }
    }
    for (run <- runList) {
      run.detectDisruptions()
    }
    for (run <- runList) {
      run.clearSections()
    }
  }

  private def assignLostTimeToSections(prevObservation: Observation, observation: Observation): Boolean = {
    for (run <- runList) {
      if (run.checkStops(prevObservation, observation)) {
        return true
      }
    }
    //Used for testing only - logs unassigned observation differences
    //    logger.debug("Unassigned observations prevLastStop {} [Route {} - {}] and lastStop {} [Route {} - {}].",
    //      Array[Object](prevObservation.getLastStopShortDesc, prevObservation.getContractRoute, prevObservation.getOperator,
    //        observation.getLastStopShortDesc, prevObservation.getContractRoute, observation.getOperator))
    return false
  }

  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  private def updateObservations(): Unit = {
    val latestFeedTimeOfDataTime = Environment.getLatestFeedTimeOfData.getTime
    for ((busId, observationList) <- busesOnRoute) {
      val sortedObservationList = observationList.sortBy(x => x.getTimeOfData)
      // difference in MILLISECONDS
      var timeDiff = Duration(latestFeedTimeOfDataTime - sortedObservationList(0).getTimeOfData.getTime, MILLISECONDS)
      while (timeDiff.toMinutes > Environment.getDataValidityTimeInMinutes && sortedObservationList.size > 0) {
        timeDiff = Duration(latestFeedTimeOfDataTime - sortedObservationList.remove(0).getTimeOfData.getTime, MILLISECONDS)
      }
      if (sortedObservationList.isEmpty) {
        //Testing purposes
        //logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      } else {
        busesOnRoute.put(busId, sortedObservationList)
      }
    }
  }

}
