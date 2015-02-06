package lbsl

import org.slf4j.LoggerFactory
import utility.Configuration

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source

/**
 * Created by Konstantin
 */
class Network {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStopMap: mutable.HashMap[String, BusStop] = new mutable.HashMap[String, BusStop]()
  private val routeMap: mutable.HashMap[String, Route] = new mutable.HashMap[String, Route]()
  //TODO: add list of disruptions

  def addObservation(observation: Observation): Unit = {
    val route = routeMap.getOrElse(observation.getContractRoute, null)
    if (route != null) {
      route.addObservation(observation)
    } else {
      logger.warn("Bus route [{}] missing from bus network.", observation.getContractRoute)
    }
  }

  def updateStatus(): Unit = {
    //TODO: Implement
    calculateDisruptions()
    //TODO:GENERATE FILE FROM THE DISRUPTIONS
  }

  private def testGetAVGStats(): Unit = {
    var counter = 0
    var sum = 0
    var max = 0
    var maxRoute = "RV!"
    var minRoute = "RV?"
    var min = 100
    for ((routeNumber, route) <- routeMap) {
      val temp = route.getOutboundStopSequence().size()
      if (temp > max) {
        max = temp
        maxRoute = routeNumber
      }
      if (temp < min) {
        min = temp
        minRoute = routeNumber
      }
      sum += temp
      counter += 1
    }
    logger.debug("Average bus stops per route: {}", (sum / counter))
    logger.debug("Longest route {} consists of {} stops", maxRoute, max)
    logger.debug("Shortest route {} consists of {} stops", minRoute, min)
  }


  private def calculateDisruptions(): Unit = {
    logger.info("BEGIN:Calculating disruptions...")
    for ((routeNumber, route) <- routeMap) {
      if (route.isRouteActive()) {
        route.update()
        if (route.getAverageDisruptionTime / 60 > 10) {
          val disruptionTime = Duration(route.getAverageDisruptionTime, SECONDS)
          logger.trace(route.getContractRoute + " - max schedule deviation change observed = " + disruptionTime.toMinutes + " minutes")
        }
      }
    }
    logger.info("FINISH:Calculating disruptions")
  }

  def init(): Unit = {
    //TODO: translate easting/northing to lat/long
    logger.info("BEGIN: Loading bus stops.")
    loadBusStops()
    logger.info("FINISH: Loaded {} bus stops.", busStopMap.size)
    logger.info("BEGIN: Loading bus routes.")
    loadRoutes()
    logger.info("FINISH: Loaded {} bus routes.", routeMap.size)
  }

  private def loadBusStops(): Unit = {
    val source = Source.fromFile(Configuration.getBusStopFile().getAbsolutePath)
    //TODO: check whether to drop headers
    for (line <- source.getLines().drop(1)) {
      val tokens: Array[String] = line.split(Configuration.getBusStopFileDelimiter)
      //TODO: This check should be more intelligent
      if (tokens.length >= 10) {
        busStopMap.put(tokens(BusStop.StopCodeLBSL), new BusStop(tokens(BusStop.StopName), tokens(BusStop.Latitude).toDouble, tokens(BusStop.Longitude).toDouble))
      }
    }
    source.close
  }

  private def loadRoutes(): Unit = {
    val source = Source.fromFile(Configuration.getBusRouteFile.getAbsolutePath)
    var routeKey: String = null
    var route: Route = null
    for (line <- source.getLines().drop(1)) {
      val tokens: Array[String] = line.split(Configuration.getBusRouteFileDelimiter)
      if (tokens.length >= 11) {
        if (route == null) {
          routeKey = tokens(Route.Route)
          route = new Route(routeKey)
        }
        if (routeKey != tokens(Route.Route)) {
          routeMap.put(routeKey, route)
          routeKey = tokens(Route.Route)
          route = new Route(routeKey)
        }
        val direction = if (Integer.parseInt(tokens(1)) % 2 == 0) Route.Inbound else Route.Outbound
        route.addBusStop(tokens(Route.StopCodeLBSL), direction, Integer.parseInt(tokens(Route.Sequence)) - 1)
      }

    }
    source.close
  }

  //TODO: Need to throw exception probably
  def getBusStop(code: String): BusStop = busStopMap.getOrElse(code, null)

  //TODO: Need to throw exception probably
  def getRoute(number: String): Route = routeMap.getOrElse(number, null)

  // USING RUNS
  //  private def loadRoutes(): Unit = {
  //    val source = Source.fromFile(routesListFile.getAbsolutePath)
  //
  //    var routeKey: String = null
  //    var route: Route = new Route("1", 1)
  //
  //    for (line <- source.getLines().drop(1)) {
  //      val tokens: Array[String] = line.split(routesListFileDelimeter)
  //      if (tokens.length >= 11) {
  //        var run = Integer.parseInt(tokens(1)) % 2
  //        if (run == 0) run = 2
  //        if (routeKey != tokens(0) + "_" + run) {
  //          routeKey = tokens(0) + "_" + run
  //          routeMap.put(routeKey, route)
  //          route = new Route(tokens(0), run)
  //        }
  //        route.addBusStop(tokens(3), Integer.parseInt(tokens(2)) - 1)
  //      }
  //
  //    }
  //    source.close
  //  }
}
