package lbsl

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Calendar

import org.slf4j.LoggerFactory
import uk.me.jstott.jcoord.OSRef
import utility.{Configuration, MissingData}

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

  private val outputFilename: String = "E:\\Workspace\\iBusNetTestDirectory\\DisruptionReports\\Report.csv"
  private val dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")

  private var prevTime: String = dateFormat.format(Calendar.getInstance().getTime())

  //TODO: add list of disruptions

  def update(): Unit = {
    calculateDisruptions()
    //TODO:GENERATE FILE FROM THE DISRUPTIONS
  }


  private def calculateDisruptions(): Unit = {
    logger.info("BEGIN:Calculating disruptions...")
    var stringToWrite = ""
    for ((routeNumber, route) <- routeMap if route.isRouteActive()) {
      route.run()
      //TODO: Capture below in a method and loop for both inbound and outbound directions
      for (i: Integer <- Array[Integer](Route.Outbound, Route.Inbound)) {
        val totalDisruptionTime = Duration(route.getTotalDisruptionTime(i), SECONDS).toMinutes
        if (totalDisruptionTime > 5) {
          val direction = Route.getDirectionString(i)
          logger.trace(route.getContractRoute + " - total " + direction + " disruption observed = " + totalDisruptionTime + " minutes")
          val list = route.getDisruptedSections(i)
          for (section <- 0 until list.size) {
            val disruptionInMinutes = list(section)._3 / 60
            if (disruptionInMinutes > 1) {
              val stopA = busStopMap.getOrElse(list(section)._1, null).getName()
              val stopB = busStopMap.getOrElse(list(section)._2, null).getName()
              val time = Configuration.getDateFormat().format(Configuration.getLatestFeedDateTime)
              stringToWrite += (route.getContractRoute + "," + direction + ",\"" + stopA + "\",\"" + stopB + "\"," + disruptionInMinutes + "," + totalDisruptionTime + ",0," + time + "\n")
              logger.trace("{} - {} disrupted section between stop [{}] and stop [{}] of [{}] seconds. ", Array[Object](route.getContractRoute, Route.getDirectionString(i), stopA, stopB, disruptionInMinutes.toString))
            }
          }
        }
      }

    }
    //TODO: check if directory exists and if not try to create it
    //TODO: java.io.FileNotFoundException: E:\Workspace\iBusNetTestDirectory\DisruptionReports\Report.csv (The process cannot access the file because it is being used by another process)
    if (stringToWrite.length > 0) {
      val file = new File(outputFilename)
      if (file.exists()) {
        file.renameTo(new File("E:\\Workspace\\iBusNetTestDirectory\\DisruptionReports\\Report_" + prevTime + ".csv"))
      }

      val fileWriter = new PrintWriter(new File(outputFilename))
      fileWriter.write("Route,Direction,SectionStart,SectionEnd,DisruptionObserved,RouteTotal,Trend,TimeFirstDetected\n" + stringToWrite)
      fileWriter.close()
      prevTime = dateFormat.format(Calendar.getInstance().getTime())
    }

    logger.info("FINISH:Calculating disruptions")
  }

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
        busStopMap.put(tokens(BusStop.StopCodeLBSL), new BusStop(tokens(BusStop.StopName), latLng.getLat, latLng.getLng))
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

    for((key, route) <- routeMap){
      route.generateSections()
    }

  }

  /**
   *
   * @param code the bus stop LBSL code
   * @return the bus stop if it exists,
   *         otherwise null
   */
  protected def getBusStop(code: String): BusStop = busStopMap.getOrElse(code, null)

  /**
   *
   * @param number the bus route number
   * @return the bus route if exists,
   *         otherwise null
   */
  protected def getRoute(number: String): Route = routeMap.getOrElse(number, null)

}
