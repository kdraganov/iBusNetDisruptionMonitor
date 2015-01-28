package lbsl

import java.util

import scala.collection.mutable

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) {

  private var averageScheduleDeaviation: Double = 0
  private val outboundBusStopSequence: util.ArrayList[String] = new util.ArrayList[String]()
  private val inboundBusStopSequence: util.ArrayList[String] = new util.ArrayList[String]()
  //  private var observationList: List = null //observation list for this route

  private val loggedBussesMap: mutable.HashMap[Integer, ObservationList[Observation]] = new mutable.HashMap[Integer, ObservationList[Observation]]()

  def addObservation(observation: Observation): Unit = {
    val observationList = loggedBussesMap.getOrElse(observation.getVehicleId, new ObservationList[Observation]())
    observationList.add(observation)
    loggedBussesMap.put(observation.getVehicleId, observationList)
  }

  def addBusStop(busStopCode: String, direction: Integer): Unit = {
    if (direction == Route.inbound) {
      inboundBusStopSequence.add(busStopCode)
    } else {
      outboundBusStopSequence.add(busStopCode)
    }
  }

  def addBusStop(busStopCode: String, direction: Integer, index: Integer): Unit = {
    if (direction == Route.inbound) {
      if (index > inboundBusStopSequence.size()) {
        inboundBusStopSequence.add(inboundBusStopSequence.size(), busStopCode)
      } else if (index < 0) {
        inboundBusStopSequence.add(0, busStopCode)
      } else {
        inboundBusStopSequence.add(index, busStopCode)
      }
    } else {
      if (index > outboundBusStopSequence.size()) {
        outboundBusStopSequence.add(outboundBusStopSequence.size(), busStopCode)
      } else if (index < 0) {
        outboundBusStopSequence.add(0, busStopCode)
      } else {
        outboundBusStopSequence.add(index, busStopCode)
      }
    }
  }


  def getInboundStopSequence(): util.ArrayList[String] = {
    return inboundBusStopSequence
  }

  def getOutboundStopSequence(): util.ArrayList[String] = {
    return outboundBusStopSequence
  }

  def getContractRoute = contractRoute

}

object Route {
  final val outbound = 1
  final val inbound = 2
}
