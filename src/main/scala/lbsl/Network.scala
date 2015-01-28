package lbsl

import java.io.File

import scala.collection.mutable
import scala.io.Source

/**
 * Created by Konstantin on 26/01/2015.
 */
class Network {

  private final val busStopListFileDelimeter = "\\t"
  private final val routesListFileDelimeter = ","

  private var busStopListFile: File = null
  private var routesListFile: File = null
  private val busStopMap: mutable.HashMap[String, BusStop] = new mutable.HashMap[String, BusStop]()
  private val routeMap: mutable.HashMap[String, Route] = new mutable.HashMap[String, Route]()

  def calculateDisruptions(): Unit ={
    println("BEGIN:Calculating disruptions...")
    println("FINISH:Calculating disruptions")
  }

  def init(busStopListFilePath: String, routesListFilePath: String): Unit = {
    busStopListFile = new File(busStopListFilePath)
    routesListFile = new File(routesListFilePath)
    //TODO: translate easting/northing to lat/long
    println("BEGIN: Loading bus stops.")
    loadBusStops()
    println("FINISH: Loaded " + busStopMap.size + " bus stops.")
    println("BEGIN: Loading bus routes.")
    loadRoutes()
    println("FINISH: Loaded " + routeMap.size + " bus routes.")

    //TEST
    //    for ((key, route) <- routeMap) {
    //      if(route == null){
    //        println(key + " - NULL ROUTE")
    //      }
    //
    //      var newKey = route.getContractRoute + "_"
    //      if (route.getDirection == Route.inbound) {
    //        newKey += Route.outbound
    //      } else {
    //        newKey += Route.inbound
    //      }
    //      if (routeMap.get(newKey) == None) {
    //        println("Missing: " + newKey)
    //      }
    //    }
    //    for ((key, stop) <- busStopMap) {
    //      println(key + "|" + stop.getName() + "|" + stop.getLatitude() + "|" + stop.getLatitude())
    //    }
    //    val stop: BusStop = busStops.getOrElse("R0865", null)
    //    println(stop.getCode() + "|" + stop.getName() + "|" + stop.getLatitude() + "|" + stop.getLatitude())

  }

  private def loadBusStops(): Unit = {
    val source = Source.fromFile(busStopListFile.getAbsolutePath)
    for (line <- source.getLines().drop(1)) {
      //drop header line
      val tokens: Array[String] = line.split(busStopListFileDelimeter)
      if (tokens.length >= 10) {
        busStopMap.put(tokens(0), new BusStop(tokens(3), tokens(9).toDouble, tokens(10).toDouble))
      }
    }
    source.close
  }

  private def loadRoutes(): Unit = {
    val source = Source.fromFile(routesListFile.getAbsolutePath)
    var routeKey: String = null
    var route: Route = new Route("1")
    for (line <- source.getLines().drop(1)) {
      val tokens: Array[String] = line.split(routesListFileDelimeter)
      if (tokens.length >= 11) {
        if (routeKey != tokens(0)) {
          routeKey = tokens(0)
          routeMap.put(routeKey, route)
          route = new Route(tokens(0))
        }
        val direction = if (Integer.parseInt(tokens(1)) % 2 == 0) Route.inbound else Route.outbound
        route.addBusStop(tokens(3), direction, Integer.parseInt(tokens(2)) - 1)
      }

    }
    source.close
  }

  //TODO: Need to throw exception probably
  def getBusStop(code: String): BusStop = busStopMap.getOrElse(code, null)

  //TODO: Need to throw exception probably
  def getRoute(number: String): Route = routeMap.getOrElse(number, null)

  def setRouteListFilePath(path: String) {
    routesListFile = new File(path)
  }

  def setBusStopListFilePath(path: String) {
    busStopListFile = new File(path)
  }

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
