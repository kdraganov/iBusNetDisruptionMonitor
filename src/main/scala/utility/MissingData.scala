package utility

import org.slf4j.LoggerFactory

import scala.collection.mutable.HashMap

/**
 * Created by Konstantin on 18/02/2015.
 */

object MissingData {
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val missingRoutes: HashMap[String, String] = new HashMap[String, String]()
  private val missingBusStops = new HashMap[String, String]()

  def addMissingRoute(route: String, operator: String): Unit = {
    if (missingRoutes.getOrElse(route, null) == null) {
      missingRoutes.put(route, operator)
      logRoutes()
    }
  }

  def addMissingStop(stop: String, route: String, file: String): Unit = {
    if (missingBusStops.getOrElse(stop, null) == null) {
      missingBusStops.put(stop, route + "_" + file)
      logStops()
    }
  }

  def logRoutes(): Unit = {
    var output = ""
    for ((route, operator) <- missingRoutes) {
      output += " | " + route + " => " + operator
    }
    logger.debug("MISSING BUS ROUTES: {}", output)
  }

  def logStops(): Unit = {
    var output = ""
    for ((stop, route) <- missingBusStops) {
      output += " | " + stop + " => " + route
    }
    logger.debug("MISSING BUS STOPS: {}", output)
  }

}
