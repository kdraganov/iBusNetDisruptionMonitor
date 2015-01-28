package lbsl

import java.text.SimpleDateFormat
import java.util

import scala.collection.mutable

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) {

  val dateFormatter: SimpleDateFormat = new SimpleDateFormat("yyyy/mm/dd hh:mm:ss")

  private var averageScheduleDeaviation: Double = 0
  private val outboundBusStopSequence: util.ArrayList[String] = new util.ArrayList[String]()
  private val inboundBusStopSequence: util.ArrayList[String] = new util.ArrayList[String]()
  //  private var observationList: List = null //observation list for this route

  private val loggedBussesMap: mutable.HashMap[Integer, ObservationList[Observation]] = new mutable.HashMap[Integer, ObservationList[Observation]]()

  def isRouteActive(): Boolean = {
    loggedBussesMap.size > 1
  }

  def updateState2(): Unit = {
    val scheduleDeviationChangeList: Array[Double] = new Array[Double](loggedBussesMap.size)
    var counter = 0
    for ((busId, observationList) <- loggedBussesMap) {
      if (observationList.size() >= 2) {
        scheduleDeviationChangeList(counter) = observationList.get(observationList.size() - 1).getScheduleDeviation - observationList.get(observationList.size() - 2).getScheduleDeviation
        counter += 1
      }
    }
    averageScheduleDeaviation = scheduleDeviationChangeList(0)
    for (index <- 1 until scheduleDeviationChangeList.size) {
      if (scheduleDeviationChangeList(index) > averageScheduleDeaviation) {
        averageScheduleDeaviation = scheduleDeviationChangeList(index)
      }
    }
  }

  def updateState(): Unit = {
    val scheduleDeviationChangeList: Array[Double] = new Array[Double](loggedBussesMap.size)
    var counter = 0
    for ((busId, observationList) <- loggedBussesMap) {
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
    averageScheduleDeaviation = sum / scheduleDeviationChangeList.length

  }

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

  def getAverageDisruptionTime = averageScheduleDeaviation
}

object Route {
  final val outbound = 1
  final val inbound = 2
}
