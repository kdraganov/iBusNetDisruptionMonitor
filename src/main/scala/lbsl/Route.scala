package lbsl

import java.util.{ArrayList, Collections}

import org.slf4j.LoggerFactory
import utility.{Configuration, Helper}

import scala.collection.mutable.HashMap

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var averageScheduleDeviation: Double = 0
  private val outboundBusStopSequence: ArrayList[String] = new ArrayList[String]()
  private val inboundBusStopSequence: ArrayList[String] = new ArrayList[String]()

  private var inboundRouteSegments: Array[Integer] = Helper.getRouteSegments(inboundBusStopSequence.size(), Configuration.getRouteSegmentSize)
  private var outboundRouteSegments: Array[Integer] = null

  private val busesOnRoute: HashMap[Integer, ArrayList[Observation]] = new HashMap[Integer, ArrayList[Observation]]()

  def generateRouteSegmentation(): Unit = {
    inboundRouteSegments = Helper.getRouteSegments(inboundBusStopSequence.size(), Configuration.getRouteSegmentSize)
    outboundRouteSegments = Helper.getRouteSegments(outboundBusStopSequence.size(), Configuration.getRouteSegmentSize)
  }

  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  //TODO: put the old date time as a settings parameter
  private def updateObservations(): Unit = {
    val update = !busesOnRoute.isEmpty
    for ((busId, observationList) <- busesOnRoute) {
      Collections.sort(observationList)
      val latestFeed = observationList.get(observationList.size() - 1)
      var diff = (latestFeed.getTimeOfData.getTime - observationList.get(0).getTimeOfData.getTime) / 60 * 60 * 1000
      while (observationList.size() > 0 && diff > 1) {
        diff = (latestFeed.getTimeOfData.getTime - observationList.remove(0).getTimeOfData.getTime) / 60 * 60 * 1000
      }
      if (observationList.isEmpty) {
        logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      }
    }
    if(update){
      logger.debug("Bus route {} has {} active buses.", contractRoute, busesOnRoute.size)
    }
  }

  def update(): Unit = {
    //TODO: NEXT STEP CALCULATE BUS PERFORMANCE PER SEGMENT AND TAKE THE WEIGHT MOVING AVERAGE
    logger.debug("Here route {} has to update its state.", contractRoute)
  }

  private def updateState2(): Unit = {
    val scheduleDeviationChangeList: Array[Double] = new Array[Double](busesOnRoute.size)
    var counter = 0
    for ((busId, observationList) <- busesOnRoute) {
      if (observationList.size() >= 2) {
        scheduleDeviationChangeList(counter) = observationList.get(observationList.size() - 1).getScheduleDeviation - observationList.get(observationList.size() - 2).getScheduleDeviation
        counter += 1
      }
    }
    averageScheduleDeviation = scheduleDeviationChangeList(0)
    for (index <- 1 until scheduleDeviationChangeList.size) {
      if (scheduleDeviationChangeList(index) > averageScheduleDeviation) {
        averageScheduleDeviation = scheduleDeviationChangeList(index)
      }
    }
  }

  private def updateState(): Unit = {
    val scheduleDeviationChangeList: Array[Double] = new Array[Double](busesOnRoute.size)
    var counter = 0
    for ((busId, observationList) <- busesOnRoute) {
      if (observationList.size() >= 2) {
        // else not enough data
        var average: Double = 0
        var sum: Double = 0
        var weightSum: Double = 0
        var prevObservation = observationList.get(0)

        for (index <- 1 until observationList.size()) {
          val observation = observationList.get(index)
          val weight = Math.pow(5, index) / 1000
          weightSum += weight
          sum += weight * (observation.getScheduleDeviation - prevObservation.getScheduleDeviation)
          prevObservation = observation

          //        println(busId + " - " + dateFormatter.format(observationList.get(index).getTimeOfData))
        }
        scheduleDeviationChangeList(counter) = sum / weightSum
        counter += 1
      }
    }
    var sum: Double = 0
    for (value: Double <- scheduleDeviationChangeList) {
      sum += value
    }
    averageScheduleDeviation = sum / scheduleDeviationChangeList.length
  }

  def addObservation(observation: Observation): Unit = {
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ObservationList[Observation]())
    observationList.add(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }

  def addBusStop(busStopCode: String, direction: Integer): Unit = {
    if (direction == Route.Inbound) {
      inboundBusStopSequence.add(busStopCode)
    } else {
      outboundBusStopSequence.add(busStopCode)
    }
  }

  def addBusStop(busStopCode: String, direction: Integer, index: Integer): Unit = {
    if (direction == Route.Inbound) {
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


  def getInboundStopSequence(): ArrayList[String] = {
    return inboundBusStopSequence
  }

  def getOutboundStopSequence(): ArrayList[String] = {
    return outboundBusStopSequence
  }

  def isRouteActive(): Boolean = {
    updateObservations()
    busesOnRoute.size > 1
  }

  def getContractRoute = contractRoute

  def getAverageDisruptionTime = averageScheduleDeviation

}

object Route {

  final val Outbound = 1
  final val Inbound = 2

  final val Route: Integer = 0
  final val Run: Integer = 1
  final val Sequence: Integer = 2
  final val StopCodeLBSL: Integer = 3
  final val BusStopCode: Integer = 4
  final val NaptanAtco: Integer = 5
  final val StopName: Integer = 6
  final val LocationEasting: Integer = 7
  final val LocationNorthing: Integer = 8
  final val Heading: Integer = 9
  final val VirtualBusStop: Integer = 10
}
