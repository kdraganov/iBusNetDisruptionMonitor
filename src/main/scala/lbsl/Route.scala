package lbsl

import java.util
import java.util.{ArrayList, Collections, Date}

import org.slf4j.LoggerFactory

import scala.collection.mutable.HashMap
import scala.concurrent.duration._

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) {

  //TODO:Move this to settings.xml
  private final val DataValidityTimeInHours: Integer = 1

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  //  private var inboundScheduleDeviation: Double = 0
  //  private var outboundScheduleDeviation: Double = 0

  private var sectionWMADelays: Array[Array[Integer]] = null

  private val busStopSequence: Array[ArrayList[String]] = Array(new util.ArrayList[String](), new util.ArrayList[String]())

  //  private val outboundBusStopSequence: ArrayList[String] = new ArrayList[String]()
  //  private val inboundBusStopSequence: ArrayList[String] = new ArrayList[String]()

  private val sections: Array[Array[ArrayList[Tuple2[Integer, Date]]]] = new Array[Array[ArrayList[Tuple2[Integer, Date]]]](2)
  //Array(Array(new util.ArrayList[(Integer, Date)]()),Array(new util.ArrayList[(Integer, Date)]()))
  //

  //  private var inboundSections: Array[ArrayList[Tuple2[Integer, Date]]] = null
  //  private var outboundSections: Array[ArrayList[Tuple2[Integer, Date]]] = null

  private val busesOnRoute: HashMap[Integer, ArrayList[Observation]] = new HashMap[Integer, ArrayList[Observation]]()

  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  private def updateObservations(): Unit = {
    val update = !busesOnRoute.isEmpty
    for ((busId, observationList) <- busesOnRoute) {
      Collections.sort(observationList)
      val latestFeed = observationList.get(observationList.size() - 1)
      var timeDiff = latestFeed.getTimeOfData.getTime - observationList.get(0).getTimeOfData.getTime // difference in MILLISECONDS
      while (Duration(timeDiff, MILLISECONDS).toHours > this.DataValidityTimeInHours && observationList.size() > 0) {
        timeDiff = latestFeed.getTimeOfData.getTime - observationList.remove(0).getTimeOfData.getTime
      }
      if (observationList.isEmpty) {
        //logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      }
    }
    if (update) {
      // logger.debug("Bus route {} has {} active buses.", contractRoute, busesOnRoute.size)
    }
  }

  private def getDirectionAndStopIndexes(stopA: String, stopB: String): Tuple3[Integer, Integer, Integer] = {
    //TODO: need to add code to handle the scenario of when the stops are from two different directions
    // need to get start and end of section and distribute the difference
    for (i <- 0 until busStopSequence.length) {
      val stopAIndex = busStopSequence(i).indexOf(stopA)
      val stopBIndex = busStopSequence(i).indexOf(stopB)
      if (stopBIndex > stopAIndex && stopAIndex > -1) {
        return new Triple[Integer, Integer, Integer](i, stopAIndex, stopBIndex)
      }
    }
    //    var stopAIndex = outboundBusStopSequence.indexOf(stopA)
    //    var stopBIndex = outboundBusStopSequence.indexOf(stopB)
    //    if (stopBIndex > stopAIndex && stopAIndex > -1) {
    //      return new Triple[Integer, Integer, Integer](Route.Outbound, stopAIndex, stopBIndex)
    //    }
    //    stopAIndex = inboundBusStopSequence.indexOf(stopA)
    //    stopBIndex = inboundBusStopSequence.indexOf(stopB)
    //    if (stopAIndex > -1 && stopBIndex > -1) {
    //      return new Triple[Integer, Integer, Integer](Route.Inbound, stopAIndex, stopBIndex)
    //    }
    logger.debug("Bus stop with LBSL id {} or {} cannot be found for route {}.", Array[Object](stopA, stopB, getContractRoute))
    return null
  }

  private def addDifference(stopA: String, stopB: String, difference: Integer, date: Date): Unit = {
    val temp = getDirectionAndStopIndexes(stopA, stopB)
    // readings not from the same direction
    if (temp != null && (temp._3 - temp._2 - 1) > 0) {
      val diff = difference / (temp._3 - temp._2 - 1)
      for (i <- (temp._2.intValue() + 1) until temp._3) {
        //logger.debug("Adding difference to inbound section {}", i)
        sections(temp._1)(i).add((diff, date))
      }

      //      if (temp._1 == Route.Inbound) {
      //        for (i <- (temp._2.intValue() + 1) until temp._3) {
      //          logger.debug("Adding difference to inbound section {}", i)
      //          inboundSections(i).add((diff, date))
      //        }
      //      } else {
      //        for (i <- (temp._2.intValue() + 1) until temp._3) {
      //          logger.debug("Adding difference to outbound section {}", i)
      //          outboundSections(i).add((diff, date))
      //        }
      //      }

    }

  }

  def generateSections(): Unit = {
    for (i <- 0 until sections.length) {
      sections(i) = new Array[ArrayList[Tuple2[Integer, Date]]](busStopSequence(i).size() - 1)
      for (x <- 0 until busStopSequence(i).size() - 1) {
        sections(i)(x) = new ArrayList[Tuple2[Integer, Date]]()
      }
    }
    //    inboundSections = new Array[ArrayList[Tuple2[Integer, Date]]](inboundBusStopSequence.size() - 1)
    //    for (i <- 0 until inboundSections.length) {
    //      inboundSections(i) = new ArrayList[Tuple2[Integer, Date]]()
    //    }
    //    outboundSections = new Array[ArrayList[Tuple2[Integer, Date]]](outboundBusStopSequence.size() - 1)
    //    for (i <- 0 until outboundSections.length) {
    //      outboundSections(i) = new ArrayList[Tuple2[Integer, Date]]()
    //    }
  }

  def update(): Unit = {
    generateSections()
    for ((busId, observationList) <- busesOnRoute) {
      if (observationList.size() > 0) {
        // logger.debug("Route {} observation list size = {} ", getContractRoute, observationList.size())
        //we need at least two observations
        var preSectionObservation = observationList.get(0)
        for (i <- 1 until observationList.size()) {
          val postSectionObservation = observationList.get(i)
          if (preSectionObservation.getLastStopShortDesc != postSectionObservation.getLastStopShortDesc) {
            val difference = postSectionObservation.getScheduleDeviation - preSectionObservation.getScheduleDeviation //schedule deviation difference in seconds
            //logger.debug("Adding stopA = {} and stopB = {} with schedule deviation difference = {} and time of data {}",
            //   Array[Object](preSectionObservation.getLastStopShortDesc, postSectionObservation.getLastStopShortDesc, difference.toString, postSectionObservation.getTimeOfData.toString))

            addDifference(preSectionObservation.getLastStopShortDesc, postSectionObservation.getLastStopShortDesc, difference, postSectionObservation.getTimeOfData)
            preSectionObservation = postSectionObservation
          }
        }
      }
    }
    calculateSectionDelay()
  }

  def calculateSectionDelay(): Unit = {
    sectionWMADelays = Array(new Array[Integer](sections(getOutboundIndex).size), new Array[Integer](sections(getInboundIndex).size))
    for (index <- 0 until sectionWMADelays.length) {
      //for each direction
      for (segmentIndex <- 0 until sections(index).length) {
        val segment = sections(index)(segmentIndex)
        if (segment != null) {
          // need to sort it by time
          val tempArray = segment.toArray(new Array[Tuple2[Integer, Date]](segment.size())).sortBy(_._2)
          var weightSum = 0
          var weight = 0
          for (i <- 0 until tempArray.length) {
            weight += (i + 1)
            weightSum += tempArray(i)._1 * (i + 1)
          }
          sectionWMADelays(index)(segmentIndex) = 0
          if (weight > 0 && (weightSum / weight) > 0) {
            sectionWMADelays(index)(segmentIndex) = weightSum / weight
          }
        }
      }
    }


    //logger.debug("Route {} inbound deviation = {} ", getContractRoute, inboundScheduleDeviation)

    //    for (x <- 0 until outboundSections.length) {
    //      val segment = outboundSections(x)
    //      if (segment != null) {
    //        val tempArray = new Array[Tuple2[Integer, Date]](segment.size())
    //        segment.toArray(tempArray).sortBy(_._2)
    //        var weightSum = 0
    //        var weight = 0
    //        for (i <- 0 until tempArray.length) {
    //          weight += (i + 1)
    //          weightSum += tempArray(i)._1 * (i + 1)
    //        }
    //        outbound(x) = 0
    //        if (weight > 0 && (weightSum / weight) > 0) {
    //          outbound(x) = weightSum / weight
    //        }
    //        outboundScheduleDeviation += outbound(x)
    //      }
    //    }
    //logger.debug("Route {} outbound deviation = {} ", getContractRoute, outboundScheduleDeviation)
  }

  def addObservation(observation: Observation): Unit = {
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ObservationList[Observation]())
    observationList.add(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }

  def addBusStop(busStopCode: String, direction: Integer): Unit = {
    busStopSequence(direction - 1).add(busStopCode)
    //
    //    if (direction == Route.Inbound) {
    //      inboundBusStopSequence.add(busStopCode)
    //    } else {
    //      outboundBusStopSequence.add(busStopCode)
    //    }
  }

  def addBusStop(busStopCode: String, direction: Integer, index: Integer): Unit = {

    if (index > busStopSequence(direction - 1).size()) {
      busStopSequence(direction - 1).add(busStopSequence(direction - 1).size(), busStopCode)
    } else if (index < 0) {
      busStopSequence(direction - 1).add(0, busStopCode)
    } else {
      busStopSequence(direction - 1).add(index, busStopCode)
    }

    //    if (direction == Route.Inbound) {
    //      if (index > inboundBusStopSequence.size()) {
    //        inboundBusStopSequence.add(inboundBusStopSequence.size(), busStopCode)
    //      } else if (index < 0) {
    //        inboundBusStopSequence.add(0, busStopCode)
    //      } else {
    //        inboundBusStopSequence.add(index, busStopCode)
    //      }
    //    } else {
    //      if (index > outboundBusStopSequence.size()) {
    //        outboundBusStopSequence.add(outboundBusStopSequence.size(), busStopCode)
    //      } else if (index < 0) {
    //        outboundBusStopSequence.add(0, busStopCode)
    //      } else {
    //        outboundBusStopSequence.add(index, busStopCode)
    //      }
    //    }

  }

  def getInboundStopSequence(): Array[String] = {
    return busStopSequence(getInboundIndex).toArray(new Array[String](busStopSequence(getInboundIndex).size()))
  }

  def getOutboundStopSequence(): Array[String] = {
    return busStopSequence(getOutboundIndex).toArray(new Array[String](busStopSequence(getOutboundIndex).size()))
  }

  def isRouteActive(): Boolean = {
    updateObservations()
    busesOnRoute.size > 1
  }

  def getContractRoute = contractRoute

  def getOutboundDisruptionTime(): Double = {
    getDisruptionTime(getOutboundIndex)
  }

  def getInboundDisruptionTime(): Double = {
    getDisruptionTime(getInboundIndex)
  }

  def getInboundDisruptedSections(): ArrayList[Tuple3[String, String, Integer]] = {
    getDisruptedSections(getInboundIndex)
  }

  def getOutboundDisruptedSections(): ArrayList[Tuple3[String, String, Integer]] = {
    getDisruptedSections(getOutboundIndex)
  }

  def getDisruptionTime(direction: Integer): Double = {
    var sum = 0
    for (section: Integer <- sectionWMADelays(direction)) {
      sum += section
    }
    return sum
  }

  def getDisruptedSections(direction: Integer): ArrayList[Tuple3[String, String, Integer]] = {
    val disruptedSections = new ArrayList[Tuple3[String, String, Integer]]()
    var stopA: String = null
    var stopB: String = null
    var disruption: Integer = 0
    for (i <- 0 until sectionWMADelays(direction).length) {
      if (sectionWMADelays(direction)(i) > 0) {
        //continue or start of disrupted section
        if (stopA == null) {
          stopA = busStopSequence(direction).get(i)
        }
        disruption += sectionWMADelays(direction)(i)
      } else if (stopA != null) {
        // end of sectionDisruption
        stopB = busStopSequence(direction).get(i)
        disruptedSections.add((stopA, stopB, disruption))
        stopA = null
        disruption = 0
      }
    }
    return disruptedSections
  }

  private def getIndex(direction: Integer): Integer = {
    if (direction == Route.Inbound) {
      return getInboundIndex
    }
    return getOutboundIndex
  }

  private def getInboundIndex = Route.Inbound - 1

  private def getOutboundIndex = Route.Outbound - 1
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


//private def updateState2(): Unit = {
//val scheduleDeviationChangeList: Array[Double] = new Array[Double](busesOnRoute.size)
//var counter = 0
//for ((busId, observationList) <- busesOnRoute) {
//if (observationList.size() >= 2) {
//scheduleDeviationChangeList(counter) = observationList.get(observationList.size() - 1).getScheduleDeviation - observationList.get(observationList.size() - 2).getScheduleDeviation
//counter += 1
//}
//}
//averageScheduleDeviation = scheduleDeviationChangeList(0)
//for (index <- 1 until scheduleDeviationChangeList.size) {
//if (scheduleDeviationChangeList(index) > averageScheduleDeviation) {
//averageScheduleDeviation = scheduleDeviationChangeList(index)
//}
//}
//}
//
//private def updateState(): Unit = {
//val scheduleDeviationChangeList: Array[Double] = new Array[Double](busesOnRoute.size)
//var counter = 0
//for ((busId, observationList) <- busesOnRoute) {
//if (observationList.size() >= 2) {
//// else not enough data
//var average: Double = 0
//var sum: Double = 0
//var weightSum: Double = 0
//var prevObservation = observationList.get(0)
//
//for (index <- 1 until observationList.size()) {
//val observation = observationList.get(index)
//val weight = Math.pow(5, index) / 1000
//weightSum += weight
//sum += weight * (observation.getScheduleDeviation - prevObservation.getScheduleDeviation)
//prevObservation = observation
//
////        println(busId + " - " + dateFormatter.format(observationList.get(index).getTimeOfData))
//}
//scheduleDeviationChangeList(counter) = sum / weightSum
//counter += 1
//}
//}
//var sum: Double = 0
//for (value: Double <- scheduleDeviationChangeList) {
//sum += value
//}
//averageScheduleDeviation = sum / scheduleDeviationChangeList.length
//}