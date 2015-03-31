package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}

import org.slf4j.LoggerFactory
import utility.{DBConnectionPool, Environment, MissingData}

import scala.collection.mutable

/**
 * Created by Konstantin
 */
class Network {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val routeMap: mutable.HashMap[String, Route] = new mutable.HashMap[String, Route]()
  private var maxExecutionTime: Double = 0

  def update(): Unit = {
    logger.info("BEGIN:Calculating disruptions...")
    val start = System.nanoTime()
    calculateDisruptions()
    val elapsedTime = (System.nanoTime() - start) / 1000000000.0
    maxExecutionTime = Math.max(maxExecutionTime, elapsedTime)
    logger.info("FINISH:Calculating disruptions. Calculation time {} seconds (Max calculation time {}).", elapsedTime, maxExecutionTime)
  }

  def getRouteCount(): Integer ={
    return routeMap.size
  }

  private def calculateDisruptions(): Unit = {
    Network.beginTransaction()
    for ((routeNumber, route) <- routeMap) {
      route.run()
    }
    Network.commit()
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
   * @param observation bservation to be added to network
   * @return true if bus route exists and observation has been added successfully,
   *         otherwise false
   */
  def addObservation(observation: Observation): Boolean = {
    //check if last stop is recognised
    //    if (busStopMap.getOrElse(observation.getLastStopShortDesc, null) == null) {
    //      //bus stop is missing no point of adding it
    //      MissingData.addMissingStop(observation.getLastStopShortDesc, observation.getContractRoute, observation.getOperator)
    //      return false
    //    }

    var route = routeMap.getOrElse(observation.getContractRoute, null)
    //check if it a 24h service bus
    if (route == null && observation.getContractRoute.startsWith("N")) {
      route = routeMap.getOrElse(observation.getContractRoute.substring(1), null)
    }
    if (route != null) {
      val tempDate = observation.getTimeOfData
      if (tempDate.getTime > Environment.getLatestFeedTime) {
        Environment.setLatestFeedDateTime(tempDate)
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
    //TODO: concurrent execution could be introduced here
    for ((routeNumber, route) <- routeMap) {
      route.init()
    }
  }

}

protected object Network {

  var connection: Connection = null
  val logger = LoggerFactory.getLogger(getClass().getSimpleName)

  def beginTransaction(): Unit = {
    try {
      connection = DBConnectionPool.getConnection()
      connection.setAutoCommit(false)
    } catch {
      case e: SQLException => LoggerFactory.getLogger(getClass().getSimpleName).error("Exception:", e)
    }
  }

  def commit(): Unit = {
    var preparedStatement: PreparedStatement = null
    val query = "UPDATE \"EngineConfigurations\" SET \"value\" = ? WHERE key = 'latestFeedTime'"
    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setString(1, Environment.getDateFormat().format(Environment.getLatestFeedTime))
      preparedStatement.executeUpdate()
    } catch {
      case e: SQLException => logger.error("Exception: with query ({}) ", preparedStatement.toString, e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
    try {
      connection.commit()
    } catch {
      case e: SQLException => logger.error("Exception:", e)
        connection.rollback()
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    connection = null
  }
}