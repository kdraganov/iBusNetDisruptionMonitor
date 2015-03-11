package lbsl

import org.slf4j.LoggerFactory
import uk.me.jstott.jcoord.OSRef
import utility.{Configuration, MissingData, OutputWriter}

import scala.collection.mutable
import scala.io.Source

/**
 * Created by Konstantin
 */
class Network {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStopMap: mutable.HashMap[String, BusStop] = new mutable.HashMap[String, BusStop]()
  private val routeMap: mutable.HashMap[String, Route] = new mutable.HashMap[String, Route]()

  def update(): Unit = {
    logger.info("BEGIN:Calculating disruptions...")
    calculateDisruptions()
    logger.info("FINISH:Calculating disruptions")
  }

  /**
   *
   * @param code the bus stop LBSL code
   * @return the bus stop if it exists,
   *         otherwise null
   */
  def getBusStop(code: String): BusStop = busStopMap.getOrElse(code, null)

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
    if (busStopMap.getOrElse(observation.getLastStopShortDesc, null) == null) {
      //bus stop is missing no point of adding it
      MissingData.addMissingStop(observation.getLastStopShortDesc, observation.getContractRoute, observation.getOperator)
      return false
    }

    var route = routeMap.getOrElse(observation.getContractRoute, null)
    //check if it a 24h service bus
    if (route == null && observation.getContractRoute.startsWith("N")) {
      route = routeMap.getOrElse(observation.getContractRoute.substring(1), null)
    }
    if (route != null) {
      val tempDate = observation.getTimeOfData
      if (tempDate.getTime > Configuration.getLatestFeedTime) {
        Configuration.setLatestFeedDateTime(tempDate)
      }
      route.addObservation(observation)
      return true
    }
    MissingData.addMissingRoute(observation.getContractRoute, observation.getOperator)
    //    logger.warn("Bus route [{}] missing from bus network.", observation.getContractRoute)
    return false
  }

  /**
   * Initializes the bus network,
   * it loads the bus stops and bus routes
   */
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
      val tokens: Array[String] = line.split(Configuration.getBusStopFileRegex)
      //TODO: This check should be more intelligent
      if (tokens.length >= BusStop.NumberOfFields) {
        val latLng = new OSRef(tokens(BusStop.LocationEasting).toDouble, tokens(BusStop.LocationNorthing).toDouble).toLatLng()
        latLng.toWGS84()
        busStopMap.put(tokens(BusStop.LBSLCode), new BusStop(tokens(BusStop.StopName), tokens(BusStop.Code), tokens(BusStop.NaptanAtco), latLng.getLat, latLng.getLng))
      }
    }
    source.close
  }

  private def loadRoutes(): Unit = {
    val source = Source.fromFile(Configuration.getBusRouteFile.getAbsolutePath)
    var routeKey: String = null
    var route: Route = null
    for (line <- source.getLines().drop(1)) {
      val tokens: Array[String] = line.split(Configuration.getBusRouteFileRegex)
      if (tokens.length >= Route.NumberOfFields) {
        if (route == null) {
          routeKey = tokens(Route.Route)
          route = new Route(routeKey)
        }
        if (routeKey != tokens(Route.Route)) {
          routeMap.put(routeKey, route)
          routeKey = tokens(Route.Route)
          route = new Route(routeKey)
        }
        val run = Integer.parseInt(tokens(Route.Run))
        //For simplicity only consider the major outbound/inbound runs
        if (run == 1) {
          route.addBusStop(tokens(Route.StopCodeLBSL), Route.Outbound, Integer.parseInt(tokens(Route.Sequence)))
        } else if (run == 2) {
          route.addBusStop(tokens(Route.StopCodeLBSL), Route.Inbound, Integer.parseInt(tokens(Route.Sequence)))
        }
      }
    }
    if (routeKey != null && route != null) {
      routeMap.put(routeKey, route)
    }
    source.close

    for ((key, route) <- routeMap) {
      route.generateSections()
    }

  }

  private def calculateDisruptions(): Unit = {
    val outputWriter = new OutputWriter()
    for ((routeNumber, route) <- routeMap if route.isRouteActive()) {
      route.run()
      for (run: Integer <- Array[Integer](Route.Outbound, Route.Inbound) if route.hasDisruption(run)) {
        val totalDisruptionTime = route.getTotalDisruptionTimeMinutes(run)
        val direction = Route.getDirectionString(run)
        for (disruption: Disruption <- route.getDisruptions(run)) {
          val stopA = busStopMap.getOrElse(disruption.getSectionStartBusStop, null)
          val stopB = busStopMap.getOrElse(disruption.getSectionEndBusStop, null)
          if (stopA != null && stopB != null) {
            outputWriter.write(routeNumber,
              direction,
              stopA,
              stopB,
              disruption.getDelayInMinutes,
              totalDisruptionTime,
              disruption.getTrend,
              disruption.getTimeFirstDetected)
          } else {
            if (stopA == null) {
              logger.debug("Cannot find stop {}", disruption.getSectionStartBusStop)
            }
            if (stopB == null) {
              logger.debug("Cannot find stop {}", disruption.getSectionEndBusStop)
            }
          }
        }
      }
    }
    outputWriter.close()
  }
}