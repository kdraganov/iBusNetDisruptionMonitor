package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}

import org.slf4j.LoggerFactory
import utility.{DBConnectionPool, Environment, MissingData}

import scala.collection.mutable

/**
 * Created by Konstantin
 *
 * This class represents a bus network.
 */
class Network {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val routeMap: mutable.HashMap[String, Route] = new mutable.HashMap[String, Route]()
  private var maxExecutionTime: Double = 0

  /**
   * Method which updates the bus network state.
   */
  def update(): Unit = {
    logger.info("BEGIN:Calculating disruptions...")
    val start = System.nanoTime()
    calculateDisruptions()
    val elapsedTime = (System.nanoTime() - start) / 1000000000.0
    maxExecutionTime = Math.max(maxExecutionTime, elapsedTime)
    logger.info("FINISH:Calculating disruptions. Calculation time {} seconds (Max calculation time {}).", elapsedTime, maxExecutionTime)
  }

  def getRouteCount(): Integer = {
    return routeMap.size
  }

  /**
   *
   * @param number the bus route number
   * @return the bus route if exists,
   *         otherwise null
   */
  def getRoute(number: String): Route = routeMap.getOrElse(number, null)

  /**
   *
   * @param observation Observation to be added to network
   * @return true if bus route exists and observation has been added successfully,
   *         otherwise false
   */
  def addObservation(observation: Observation): Boolean = {
    var route = routeMap.getOrElse(observation.getContractRoute, null)
    //Check if it is a 24h service bus
    if (route == null && observation.getContractRoute.startsWith("N")) {
      route = routeMap.getOrElse(observation.getContractRoute.substring(1), null)
    }
    if (route != null) {
      val tempDate = observation.getTimeOfData
      if (tempDate.getTime > Environment.getLatestFeedTimeOfData.getTime) {
        Environment.setLatestFeedTimeOfData(tempDate)
      }
      route.addObservation(observation)
      return true
    }
    MissingData.addMissingRoute(observation.getContractRoute, observation.getOperator)
    return false
  }

  /**
   * Initializes the bus network,
   * it loads the bus stops and bus routes
   */
  def init(): Unit = {
    logger.info("BEGIN: Loading bus routes.")
    loadRoutes()
    logger.info("FINISH: Loaded {} bus routes.", routeMap.size)
  }

  private def calculateDisruptions(): Unit = {
    Environment.getDBTransaction.begin()
    for ((routeNumber, route) <- routeMap) {
      route.run()
    }
    var preparedStatement: PreparedStatement = null
    val query = "UPDATE \"EngineConfigurations\" SET \"value\" = ? WHERE key = 'latestFeedTime'"
    try {
      preparedStatement = Environment.getDBTransaction.getConnection.prepareStatement(query)
      preparedStatement.setString(1, Environment.getDateFormat().format(Environment.getLatestFeedTimeOfData))
      preparedStatement.executeUpdate()
    } catch {
      case e: SQLException => logger.error("Exception: with query ({}) ", preparedStatement.toString, e)
        logger.error("Terminating application.")
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
    Environment.getDBTransaction.commit()
  }

  private def loadRoutes(): Unit = {
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    val query = "SELECT DISTINCT route FROM \"BusRouteSequences\""
    try {
      connection = DBConnectionPool.getConnection()
      preparedStatement = connection.prepareStatement(query)
      val routesSet = preparedStatement.executeQuery()
      while (routesSet.next()) {
        routeMap.put(routesSet.getString("route"), new Route(routesSet.getString("route")))
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
    //TODO: Concurrent execution could be introduced here
    for ((routeNumber, route) <- routeMap) {
      route.init()
    }
  }

}