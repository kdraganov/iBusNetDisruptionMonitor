package lbsl

import java.util

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String, private val direction: Integer) {

  private var averageScheduleDeaviation: Double = 0

  private val busStopSequence: util.ArrayList[String] = new util.ArrayList[String]()
  //  private var busList: List = null //busses currently logged on this route - maybe redundant
  //  private var observationList: List = null //observation list for this route


  def addBusStop(busStopCode: String): Unit = {
    busStopSequence.add(busStopCode)
  }

  def addBusStop(busStopCode: String, index: Integer): Unit = {
    if (index > busStopSequence.size()) {
      busStopSequence.add(busStopSequence.size(), busStopCode)
    } else if (index < 0) {
      busStopSequence.add(0, busStopCode)
    } else {
      busStopSequence.add(index, busStopCode)
    }
  }

  def getRouteStopSequence(): util.ArrayList[String] = {
    return busStopSequence
  }

  def getContractRoute = contractRoute

  def getDirection = direction

  def getDirectionString(): String = {
    if (direction == Route.inbound) {
      return "Inbound"
    }
    return "Outbound"

  }
}

object Route {
  final val outbound = 1
  final val inbound = 2
}
