package lbsl

import java.util.Date

import org.slf4j.LoggerFactory
import utility.Configuration

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.concurrent.duration._

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStopSequence: Array[ArrayBuffer[String]] = Array(new ArrayBuffer[String](), new ArrayBuffer[String]())
  private val busesOnRoute: HashMap[Integer, ArrayBuffer[Observation]] = new HashMap[Integer, ArrayBuffer[Observation]]()

  //Section WMA delay in minutes
  private var sectionWMADelays: Array[Array[Double]] = null
  private val sections: Array[Array[ArrayBuffer[Tuple2[Double, Date]]]] = new Array[Array[ArrayBuffer[Tuple2[Double, Date]]]](2)

  private val disruptionList: Array[ArrayBuffer[Disruption]] = new Array[ArrayBuffer[Disruption]](2)

  private val totalDelay: Array[Integer] = Array(0, 0)

  override
  def run(): Unit = {
    clearSections()
    //we need at least two observations
    for ((busId, observationList) <- busesOnRoute if observationList.size > 1) {
      // logger.trace("Route {} observation list size = {} ", getContractRoute, observationList.size())
      var prevObservation = observationList(0)
      for (i <- 1 until observationList.size) {
        val observation = observationList(i)
        val scheduleDeviationDifference = observation.getScheduleDeviation - prevObservation.getScheduleDeviation
        //not interested if bus has gained time
        if (scheduleDeviationDifference > 0) {
          calculateChange(prevObservation, observation, scheduleDeviationDifference)
        }
        prevObservation = observation
      }
    }
    calculateWMASectionDelay()
    calculateTotalDelayPerRun()

    //TODO:MOVE THIS TO INIT METHOD
    disruptionList(0) = new ArrayBuffer[Disruption]
    disruptionList(1) = new ArrayBuffer[Disruption]
    for (runIndex <- 0 until sectionWMADelays.length if totalDelay(runIndex) >= Configuration.getSectionMediumThreshold) {
      findDisruptedSections(runIndex, Configuration.getSectionMinThreshold)
      //check for possible diversions

      if (disruptionList(runIndex).isEmpty && totalDelay(runIndex) > Configuration.getRouteSeriousThreshold) {
        findDisruptedSections(runIndex, 120)
      }

      if (disruptionList(runIndex).isEmpty && totalDelay(runIndex) > Configuration.getRouteSeriousThreshold) {
        findDisruptedSections(runIndex, 90)
      }

      if (disruptionList(runIndex).isEmpty && totalDelay(runIndex) > Configuration.getRouteSeriousThreshold) {
        var max = 0
        for (section <- sectionWMADelays(runIndex)) {
          if (section > max) {
            max = section.toInt
          }
        }
        max = (max / 60)
        logger.debug("Route {} direction {} disrupted by {} minutes [max section disruption is {}].", Array[Object](contractRoute, Route.getDirectionString(runIndex + 1), (totalDelay(runIndex) / 60).toString, max.toString))
      }
    }
  }

  def hasDisruption(run: Integer): Boolean = {
    return !disruptionList(getIndex(run)).isEmpty
  }

  def getDisruptions(run: Integer): ArrayBuffer[Disruption] = {
    return disruptionList(getIndex(run))
  }

  /**
   *
   * @return boolean true if there are active (e.g. have received reading in the past 1h or so) buses on the route
   *         false otherwise
   */
  def isRouteActive(): Boolean = {
    updateObservations()
    !busesOnRoute.isEmpty
  }


  def generateSections(): Unit = {
    for (i <- 0 until sections.length) {
      //TODO:Init only the direction the route has - waste of space otherwise
      sections(i) = new Array[ArrayBuffer[Tuple2[Double, Date]]](Math.max(busStopSequence(i).size - 1, 0))
      for (x <- 0 until busStopSequence(i).size - 1) {
        sections(i)(x) = new ArrayBuffer[Tuple2[Double, Date]]()
      }
    }
  }

  def addObservation(observation: Observation): Unit = {
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ArrayBuffer[Observation]())
    observationList.append(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }

  def addBusStop(busStopCode: String, direction: Integer): Unit = {
    busStopSequence(direction - 1).append(busStopCode)
  }

  def addBusStop(busStopCode: String, direction: Integer, sequence: Integer): Unit = {
    val index = sequence - 1
    val directionIndex = direction - 1
    if (index > busStopSequence(directionIndex).size) {
      busStopSequence(directionIndex).insert(busStopSequence(directionIndex).size, busStopCode)
    } else if (index < 0) {
      busStopSequence(directionIndex).insert(0, busStopCode)
    } else {
      busStopSequence(directionIndex).insert(index, busStopCode)
    }
  }

  def getInboundStopSequence(): ArrayBuffer[String] = {
    return busStopSequence(getInboundIndex)
  }

  def getOutboundStopSequence(): ArrayBuffer[String] = {
    return busStopSequence(getOutboundIndex)
  }

  def getContractRoute = contractRoute

  def getTotalDisruptionTimeMinutes(direction: Integer): Integer = {
    return Math.round(getTotalDisruptionTime(direction) / 60)
  }

  def getTotalDisruptionTime(direction: Integer): Integer = {
    return totalDelay(getIndex(direction))
  }

  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  private def updateObservations(): Unit = {
    for ((busId, observationList) <- busesOnRoute) {
      val sortedObservationList = observationList.sortBy(x => x.getTimeOfData)
      // difference in MILLISECONDS
      var timeDiff = Duration(Configuration.getLatestFeedTime - sortedObservationList(0).getTimeOfData.getTime, MILLISECONDS)
      while (timeDiff.toHours > Configuration.getDataValidityTimeInHours && sortedObservationList.size > 0) {
        timeDiff = Duration(Configuration.getLatestFeedTime - sortedObservationList.remove(0).getTimeOfData.getTime, MILLISECONDS)
      }
      if (sortedObservationList.isEmpty) {
        //logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      } else {
        busesOnRoute.put(busId, sortedObservationList)
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
    //logger.debug("Bus stop with LBSL id {} or {} cannot be found for route {}.", Array[Object](prevLastStop, lastStop, getContractRoute))
    return null
  }

  private def calculateChange(prevObservation: Observation, observation: Observation, scheduleDeviationDifference: Double): Unit = {
    val temp = getDirectionAndStopIndexes(prevObservation.getLastStopShortDesc, observation.getLastStopShortDesc)
    var direction: Integer = 0
    var prevLastStopIndex: Integer = 0
    var lastStopIndex: Integer = 0
    if (temp != null) {
      direction = temp._1
      prevLastStopIndex = temp._2
      lastStopIndex = temp._3
    }
    // TODO: consider readings not from the same direction
    if ((lastStopIndex - prevLastStopIndex - 1) > 0) {
      val scheduleDeviationSectionDistribution = scheduleDeviationDifference / (lastStopIndex - prevLastStopIndex + 1)
      for (i <- prevLastStopIndex.intValue() to Math.min(lastStopIndex, busStopSequence(direction).size - 2)) {
        sections(direction)(i).append((scheduleDeviationSectionDistribution, observation.getTimeOfData))
      }
    }

  }

  //TODO: check for diversions
  private def findDisruptedSections(runIndex: Integer, sectionMinThreshold: Integer) {
    var sectionStartStopIndex: Integer = null
    var disruptionSeconds: Double = 0
    for (i <- 0 until sectionWMADelays(runIndex).length) {
      if (sectionWMADelays(runIndex)(i) > sectionMinThreshold) {
        //continue or start of disrupted section
        if (sectionStartStopIndex == null) {
          sectionStartStopIndex = i
        }
        //          else {
        //            // large section - possible diversion
        //            if (i - sectionStartStopIndex >= Configuration.getMaxSectionLength) {
        //              if (disruptionSeconds < Configuration.getSectionMediumThreshold) {
        //                //not interested - shift start of the section
        //                disruptionSeconds -= sectionWMADelays(run)(sectionStartStopIndex)
        //                sectionStartStopIndex += 1
        //              } else {
        //                addDisruption(run, sectionStartStopIndex, i, disruptionSeconds)
        //                sectionStartStopIndex = null
        //                disruptionSeconds = 0
        //              }
        //            }
        //          }
        disruptionSeconds += sectionWMADelays(runIndex)(i)
      } else {
        if (sectionStartStopIndex != null) {
          // end of sectionDisruption
          if (disruptionSeconds >= Configuration.getSectionMediumThreshold) {
            addDisruption(runIndex, sectionStartStopIndex, i, disruptionSeconds)
          }
          sectionStartStopIndex = null
          disruptionSeconds = 0
        }
      }
    }
  }

  private def calculateWMASectionDelay(): Unit = {
    sectionWMADelays = Array(new Array[Double](sections(getOutboundIndex).size), new Array[Double](sections(getInboundIndex).size))
    for (direction <- 0 until sectionWMADelays.length) {
      //for each direction
      for (segmentIndex <- 0 until sections(direction).length) {
        val segment = sections(direction)(segmentIndex)
        if (segment != null) {
          // need to sort it by time
          val segmentSorted = segment.sortBy(_._2)
          var weightedSum: Double = 0
          var totalWeight: Double = 0
          for (i <- 0 until segmentSorted.length) {
            val weight = getWeight(i)
            totalWeight += weight
            weightedSum += segmentSorted(i)._1 * weight
          }
          sectionWMADelays(direction)(segmentIndex) = 0
          if (totalWeight > 0) {
            sectionWMADelays(direction)(segmentIndex) = weightedSum / totalWeight
          }
        }
      }
    }
  }

  private def calculateTotalDelayPerRun(): Unit = {
    for (direction <- 0 until sectionWMADelays.length) {
      var sum: Double = 0
      for (section <- sectionWMADelays(direction)) {
        sum += section
      }
      totalDelay(direction) = Math.round(sum).toInt
    }
  }

  private def getWeight(itemOrder: Integer): Double = {
    return itemOrder + 1
  }


  private def addDisruption(runIndex: Integer, sectionStartStopIndex: Integer, sectionEndStopIndex: Integer, delaySeconds: Double): Unit = {
    val disruption: Disruption = new Disruption(busStopSequence(runIndex)(sectionStartStopIndex), busStopSequence(runIndex)(sectionEndStopIndex), delaySeconds)
    disruptionList(runIndex).append(disruption)
  }

  private def getIndex(direction: Integer): Integer = {
    if (direction == Route.Inbound) {
      return getInboundIndex
    }
    return getOutboundIndex
  }

  private def getInboundIndex = Route.Inbound - 1

  private def getOutboundIndex = Route.Outbound - 1

  private def clearSections(): Unit = {
    for (section <- sections) {
      for (buffer <- section) {
        buffer.clear()
      }
    }
  }

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
  final val BusStopHeading: Integer = 9
  final val VirtualBusStop: Integer = 10

  final val NumberOfFields: Integer = 11

  def getDirectionString(direction: Integer): String = {
    if (direction == Inbound) {
      return "Inbound"
    }
    return "Outbound"
  }
}