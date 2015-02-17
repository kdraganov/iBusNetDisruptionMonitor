package lbsl

import java.util
import java.util.{ArrayList, Collections, Date}

import org.slf4j.LoggerFactory
import utility.Configuration

import scala.collection.mutable.HashMap
import scala.concurrent.duration._

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStopSequence: Array[ArrayList[String]] = Array(new util.ArrayList[String](), new util.ArrayList[String]())
  private val busesOnRoute: HashMap[Integer, ArrayList[Observation]] = new HashMap[Integer, ArrayList[Observation]]()

  private var sectionWMADelays: Array[Array[Double]] = null
  private val sections: Array[Array[ArrayList[Tuple2[Double, Date]]]] = new Array[Array[ArrayList[Tuple2[Double, Date]]]](2)

  private var totalDelay: Array[Double] = Array[Double](0, 0)

  /**
   *
   * @return boolean true if there are active (e.g. have received reading in the past 1h or so) buses on the route
   *         false otherwise
   */
  def isRouteActive(): Boolean = {
    updateObservations()
    !busesOnRoute.isEmpty //busesOnRoute.size > 1
  }

  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  private def updateObservations(): Unit = {
    for ((busId, observationList) <- busesOnRoute) {
      Collections.sort(observationList)
      // difference in MILLISECONDS
      var timeDiff = Duration(Configuration.getLatestFeedTime - observationList.get(0).getTimeOfData.getTime, MILLISECONDS)
      while (timeDiff.toHours > Configuration.getDataValidityTimeInHours && observationList.size() > 0) {
        timeDiff = Duration(Configuration.getLatestFeedTime - observationList.remove(0).getTimeOfData.getTime, MILLISECONDS)
      }
      if (observationList.isEmpty) {
        //logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      }
    }
  }

  private def getDirectionAndStopIndexes(prevLastStop: String, lastStop: String): Tuple3[Integer, Integer, Integer] = {
    //TODO: need to add code to handle the scenario of when the stops are from two different directions
    //TODO: what if the stops are the same - which direction?
    // need to get start and end of section and distribute the difference
    for (i <- 0 until busStopSequence.length) {
      val prevLastStopIndex = busStopSequence(i).indexOf(prevLastStop)
      val lastStopIndex = busStopSequence(i).indexOf(lastStop)
      if (lastStopIndex >= prevLastStopIndex && prevLastStopIndex > -1) {
        return new Triple[Integer, Integer, Integer](i, prevLastStopIndex, lastStopIndex)
      }
    }
    logger.debug("Bus stop with LBSL id {} or {} cannot be found for route {}.", Array[Object](prevLastStop, lastStop, getContractRoute))
    return null
  }

  private def calculateChange(prevObservation: Observation, observation: Observation, timeLostInSeconds: Double): Unit = {
    val temp = getDirectionAndStopIndexes(prevObservation.getLastStopShortDesc, observation.getLastStopShortDesc)
    // readings not from the same direction
    if (temp != null && (temp._3 - temp._2 - 1) > 0) {
      val timeLostInSecondsPerSection = timeLostInSeconds / (temp._3 - temp._2 + 1)
      for (i <- temp._2.intValue() to Math.min(temp._3.intValue(), busStopSequence(temp._1).size() - 2)) {
        //logger.debug("Adding difference to inbound section {}", i)
        sections(temp._1)(i).add((timeLostInSecondsPerSection, observation.getTimeOfData))
      }
    }

  }

  //  private def addDifference(stopA: String, stopB: String, difference: Integer, date: Date): Unit = {
  //    val temp = getDirectionAndStopIndexes(stopA, stopB)
  //    // readings not from the same direction
  //    if (temp != null && (temp._3 - temp._2 - 1) > 0) {
  //      val diff = difference / (temp._3 - temp._2 - 1)
  //      for (i <- (temp._2.intValue() + 1) until temp._3) {
  //        //logger.debug("Adding difference to inbound section {}", i)
  //        sections(temp._1)(i).add((diff, date))
  //      }
  //    }
  //
  //  }

  private def generateSections(): Unit = {
    for (i <- 0 until sections.length) {
      sections(i) = new Array[ArrayList[Tuple2[Double, Date]]](busStopSequence(i).size() - 1)
      for (x <- 0 until busStopSequence(i).size() - 1) {
        sections(i)(x) = new ArrayList[Tuple2[Double, Date]]()
      }
    }
  }

  def update(): Unit = {
    generateSections()
    //we need at least two observations
    for ((busId, observationList) <- busesOnRoute if observationList.size() > 1) {
      //      logger.trace("Route {} observation list size = {} ", getContractRoute, observationList.size())
      var prevObservation = observationList.get(0)
      for (i <- 1 until observationList.size()) {
        val observation = observationList.get(i)
        val timeLostInSeconds = observation.getScheduleDeviation - prevObservation.getScheduleDeviation
        //not interested if bus has gained time
        if (timeLostInSeconds > 0) {
          calculateChange(prevObservation, observation, timeLostInSeconds)
        }
        //        if (prevObservation.getLastStopShortDesc != observation.getLastStopShortDesc) {
        //          //INCORRECT
        //          val difference = observation.getScheduleDeviation - prevObservation.getScheduleDeviation //schedule deviation difference in seconds
        //
        //          //logger.debug("Adding stopA = {} and stopB = {} with schedule deviation difference = {} and time of data {}",
        //          //   Array[Object](preSectionObservation.getLastStopShortDesc, postSectionObservation.getLastStopShortDesc, difference.toString, postSectionObservation.getTimeOfData.toString))
        //
        //          addDifference(prevObservation.getLastStopShortDesc, observation.getLastStopShortDesc, difference, observation.getTimeOfData)
        prevObservation = observation
        //        }
      }
    }
    calculateSectionDelay()
    calculateTotalDisruption()
  }

  def calculateSectionDelay(): Unit = {
    sectionWMADelays = Array(new Array[Double](sections(getOutboundIndex).size), new Array[Double](sections(getInboundIndex).size))
    for (direction <- 0 until sectionWMADelays.length) {
      //for each direction
      for (segmentIndex <- 0 until sections(direction).length) {
        val segment = sections(direction)(segmentIndex)
        if (segment != null) {
          // need to sort it by time
          val tempArray = segment.toArray(new Array[Tuple2[Double, Date]](segment.size())).sortBy(_._2)
          var weightSum: Double = 0
          var weight = 0
          for (i <- 0 until tempArray.length) {
            weight += (i + 1)
            weightSum += tempArray(i)._1 * (i + 1)
          }
          sectionWMADelays(direction)(segmentIndex) = 0
          if (weight > 0) {
            sectionWMADelays(direction)(segmentIndex) = weightSum / weight
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
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ArrayList[Observation]())
    observationList.add(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }

  def addBusStop(busStopCode: String, direction: Integer): Unit = {
    busStopSequence(direction - 1).add(busStopCode)
  }

  def addBusStop(busStopCode: String, direction: Integer, index: Integer): Unit = {
    if (index > busStopSequence(direction - 1).size()) {
      busStopSequence(direction - 1).add(busStopSequence(direction - 1).size(), busStopCode)
    } else if (index < 0) {
      busStopSequence(direction - 1).add(0, busStopCode)
    } else {
      busStopSequence(direction - 1).add(index, busStopCode)
    }
  }

  def getInboundStopSequence(): Array[String] = {
    return busStopSequence(getInboundIndex).toArray(new Array[String](busStopSequence(getInboundIndex).size()))
  }

  def getOutboundStopSequence(): Array[String] = {
    return busStopSequence(getOutboundIndex).toArray(new Array[String](busStopSequence(getOutboundIndex).size()))
  }

  def getContractRoute = contractRoute

  def getTotalDisruptionTime(direction: Integer): Double = totalDelay(getIndex(direction))

  private def calculateTotalDisruption(): Unit = {
    for (i <- 0 until totalDelay.length) {
      var sum: Double = 0
      for (section: Double <- sectionWMADelays(i)) {
        sum += section
      }
      totalDelay(i) = sum
    }
  }

  def getDisruptedSections(direction: Integer): ArrayList[Tuple3[String, String, Double]] = {
    val disruptedSections = new ArrayList[Tuple3[String, String, Double]]()
    var stopA: String = null
    var stopB: String = null
    var disruption: Double = 0
    //TODO: consider only small sections e.g. 3-5 stops - probably best to link it to the feed interval
    for (i <- 0 until sectionWMADelays(getIndex(direction)).length) {
      if (sectionWMADelays(getIndex(direction))(i) > 60 * 2) {
        //continue or start of disrupted section
        if (stopA == null) {
          stopA = busStopSequence(getIndex(direction)).get(i)
        }
        disruption += sectionWMADelays(getIndex(direction))(i)
      } else if (stopA != null) {
        // end of sectionDisruption
        stopB = busStopSequence(getIndex(direction)).get(i)
        if (disruption > 10 * 60) {
          disruptedSections.add((stopA, stopB, disruption))
        }
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

  final val NumberOfFields: Integer = 11

  def getDirectionString(direction: Integer): String = {
    if (direction == Inbound) {
      return "Inbound"
    }
    return "Outbound"
  }
}