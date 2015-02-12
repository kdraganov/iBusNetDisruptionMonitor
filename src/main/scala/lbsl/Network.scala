package lbsl

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Calendar

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

  private val outputFilename: String = "E:\\Workspace\\disruptions\\DisruptionReport.csv"
  private var prevTime: String = null
  //TODO: add list of disruptions

  private val dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss")

  def updateStatus(): Unit = {
    calculateDisruptions()
    //TODO:GENERATE FILE FROM THE DISRUPTIONS
  }


  private def calculateDisruptions(): Unit = {
    logger.info("BEGIN:Calculating disruptions...")
    var stringToWrite = ""
    for ((routeNumber, route) <- routeMap) {
      if (route.isRouteActive()) {
        route.update()
        //TODO: Capture below in a method and loop for both inbound and outbound directions
        var disruptionTime = Duration(route.getInboundDisruptionTime, SECONDS).toSeconds
        if (disruptionTime > 600) {
          logger.trace(route.getContractRoute + " - inbound disruption observed = " + disruptionTime + " seconds")
          val list = route.getInboundDisruptedSections()
          for (i <- 0 until list.size()) {
            if (list.get(i)._3 / 60 > 1) {
              val stopA = busStopMap.getOrElse(list.get(i)._1, null).getName()
              val stopB = busStopMap.getOrElse(list.get(i)._2, null).getName()
              stringToWrite += (route.getContractRoute + ";Inbound;" + stopA + ";" + stopB + ";" + (list.get(i)._3 / 60).toInt + ";0;2015/02/12 09:30:55\n")
              logger.trace(route.getContractRoute + " - inbound disrupted section between stop [{}] and stop [{}] of [{}] seconds. ", Array[Object](stopA, stopB, list.get(i)._3))
            }
          }
        }

        disruptionTime = Duration(route.getOutboundDisruptionTime, SECONDS).toSeconds
        if (disruptionTime > 600) {
          logger.trace(route.getContractRoute + " - outbound disruption observed  = " + disruptionTime + " seconds")
          val list = route.getOutboundDisruptedSections()
          for (i <- 0 until list.size()) {
            if (list.get(i)._3 / 60 > 1) {
              val stopA = busStopMap.getOrElse(list.get(i)._1, null).getName()
              val stopB = busStopMap.getOrElse(list.get(i)._2, null).getName()
              stringToWrite += (route.getContractRoute + ";Outbound;" + stopA + ";" + stopB + ";" + (list.get(i)._3 / 60).toInt + ";-1;2015/02/12 09:30:55\n")
              logger.trace(route.getContractRoute + " - outbound disrupted section between stop [{}] and stop [{}] of [{}] seconds. ", Array[Object](stopA, stopB, list.get(i)._3))
            }
          }
        }

      }
    }

    if (stringToWrite.length > 0) {
      val file = new File(outputFilename)
      if (file.exists()) {
        file.renameTo(new File("E:\\Workspace\\disruptions\\DisruptionReport_" + prevTime + ".csv"))
      }

      val fileWriter = new PrintWriter(new File(outputFilename))
      fileWriter.write("Route;Direction;SectionStart;SectionEnd;DisruptionObserved;Trend;TimeFirstDetected\n" + stringToWrite)
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
    val route = routeMap.getOrElse(observation.getContractRoute, null)
    if (route != null) {
      route.addObservation(observation)
      return true
    }
    logger.warn("Bus route [{}] missing from bus network.", observation.getContractRoute)
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

  //  private def testGetAVGStats(): Unit = {
  //    var counter = 0
  //    var sum = 0
  //    var max = 0
  //    var maxRoute = "RV!"
  //    var minRoute = "RV?"
  //    var min = 100
  //    for ((routeNumber, route) <- routeMap) {
  //      val temp = route.getOutboundStopSequence().size()
  //      if (temp > max) {
  //        max = temp
  //        maxRoute = routeNumber
  //      }
  //      if (temp < min) {
  //        min = temp
  //        minRoute = routeNumber
  //      }
  //      sum += temp
  //      counter += 1
  //    }
  //    logger.debug("Average bus stops per route: {}", (sum / counter))
  //    logger.debug("Longest route {} consists of {} stops", maxRoute, max)
  //    logger.debug("Shortest route {} consists of {} stops", minRoute, min)
  //  }
}
